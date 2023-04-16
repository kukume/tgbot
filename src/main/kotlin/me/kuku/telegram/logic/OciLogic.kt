package me.kuku.telegram.logic

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.auth.StringPrivateKeySupplier
import com.oracle.bmc.core.BlockstorageClient
import com.oracle.bmc.core.ComputeClient
import com.oracle.bmc.core.VirtualNetworkClient
import com.oracle.bmc.core.model.*
import com.oracle.bmc.core.requests.*
import com.oracle.bmc.identity.IdentityClient
import com.oracle.bmc.identity.model.AvailabilityDomain
import com.oracle.bmc.identity.model.Compartment
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest
import com.oracle.bmc.identity.requests.ListCompartmentsRequest
import me.kuku.telegram.entity.OciEntity
import me.kuku.utils.base64Encode

object OciLogic {

    fun provider(ociEntity: OciEntity): SimpleAuthenticationDetailsProvider? {
        return SimpleAuthenticationDetailsProvider.builder()
            .tenantId(ociEntity.tenantId)
            .userId(ociEntity.userid)
            .fingerprint(ociEntity.fingerprint)
            .privateKeySupplier(
                StringPrivateKeySupplier(ociEntity.privateKey)
            )
            .passPhrase(null)
            .build()
    }

    fun identityClient(ociEntity: OciEntity): IdentityClient {
        return IdentityClient.builder()
            .region(ociEntity.region)
            .build(provider(ociEntity))
    }

    fun virtualNetworkClient(ociEntity: OciEntity): VirtualNetworkClient {
        return VirtualNetworkClient.builder().region(ociEntity.region)
            .build(provider(ociEntity))
    }

    fun computeClient(ociEntity: OciEntity): ComputeClient {
        return ComputeClient.builder().region(ociEntity.region)
            .build(provider(ociEntity))
    }

    fun blockstorageClient(ociEntity: OciEntity): BlockstorageClient {
        return BlockstorageClient.builder().region(ociEntity.region)
            .build(provider(ociEntity))
    }

    fun listCompartments(ociEntity: OciEntity): List<Compartment> {
        val response = identityClient(ociEntity).listCompartments(
            ListCompartmentsRequest.builder()
            .compartmentId(ociEntity.tenantId).build())
        return response.items
    }

    fun firstCompartment(ociEntity: OciEntity): String {
        return listCompartments(ociEntity).first().id
    }

    /**
     * Oracle Linux
     * Canonical Ubuntu
     * CentOS
     * Windows Server 付费
     * AlmaLinux OS
     * Rocky Linux
     *
     * Oracle-Linux-9.1 Oracle-Linux-9.0 Oracle-Linux-8.7 Oracle-Linux-8.6 Oracle-Linux-7.9 Oracle-Linux-6.10
     * Canonical-Ubuntu-22.04 Canonical-Ubuntu-20.04 Canonical-Ubuntu-18.04
     * CentOS-8 CentOS-7
     * aarch64
     */
    fun listImage(ociEntity: OciEntity, operatingSystem: String? = null, displayName: String? = null,
                  operatingSystemVersion: String? = null): List<Image> {
        val response = computeClient(ociEntity).listImages(
            ListImagesRequest.builder()
                .compartmentId(firstCompartment(ociEntity))
                .operatingSystem(operatingSystem)
                .displayName(displayName)
                .operatingSystemVersion(operatingSystemVersion)
                .build()
        )
        return response.items
    }

    fun createVcn(ociEntity: OciEntity, cidrBlocks: List<String> = listOf("10.0.0.0/16")): Vcn {
        val response = virtualNetworkClient(ociEntity).createVcn(
            CreateVcnRequest.builder()
                .createVcnDetails(
                    CreateVcnDetails.builder()
                        .compartmentId(ociEntity.tenantId)
                        .cidrBlocks(cidrBlocks)
                        .build()
                ).build()
        )
        return response.vcn
    }

    fun createSubnet(ociEntity: OciEntity, vcnId: String, cidrBlock: String = "10.0.0.0/24"): Subnet {
        val response = virtualNetworkClient(ociEntity).createSubnet(
            CreateSubnetRequest.builder()
                .createSubnetDetails(
                    CreateSubnetDetails.builder()
                        .compartmentId(ociEntity.tenantId)
                        .vcnId(vcnId)
                        .cidrBlock(cidrBlock)
                        .build()
                )
                .build()
        )
        return response.subnet
    }

    fun createVcnAndSubnet(ociEntity: OciEntity, cidrBlocks: List<String> = listOf("10.0.0.0/16"), cidrBlock: String = "10.0.0.0/24"): Subnet {
        val vcn = createVcn(ociEntity, cidrBlocks)
        val id = vcn.id
        return createSubnet(ociEntity, id, cidrBlock)
    }

    fun listSubnets(ociEntity: OciEntity): List<Subnet> {
        val response = virtualNetworkClient(ociEntity).listSubnets(
            ListSubnetsRequest.builder()
                .compartmentId(ociEntity.tenantId).build()
        )
        return response.items
    }

    fun listAvailabilityDomains(ociEntity: OciEntity): List<AvailabilityDomain> {
        val response = identityClient(ociEntity).listAvailabilityDomains(
            ListAvailabilityDomainsRequest.builder()
            .compartmentId(firstCompartment(ociEntity)).build())
        return response.items
    }

    fun firstAvailabilityDomains(ociEntity: OciEntity): AvailabilityDomain {
        return listAvailabilityDomains(ociEntity)[0]
    }

    /**
     * VM.Standard.E2.1.Micro   amd
     * VM.Standard.A1.Flex  arm
     */
    fun launchInstance(ociEntity: OciEntity, imageId: String, cpu: Float, memory: Float, volumeSize: Long, shape: String, rootPassword: String): Instance {
        val listSubnets = listSubnets(ociEntity)
        val subnetId = if (listSubnets.isEmpty()) {
            createVcnAndSubnet(ociEntity).id
        } else listSubnets[0].id
        val availabilityDomain = listAvailabilityDomains(ociEntity)[0].name
        val details = LaunchInstanceDetails.builder()
            .availabilityDomain(availabilityDomain)
            .compartmentId(ociEntity.tenantId)
            .createVnicDetails(CreateVnicDetails.builder()
                .assignPublicIp(true).assignPrivateDnsRecord(true)
                .subnetId(subnetId).build())
            .shape(shape)
            .shapeConfig(LaunchInstanceShapeConfigDetails.builder()
                .ocpus(cpu).memoryInGBs(memory).build())
            .sourceDetails(InstanceSourceViaImageDetails.builder()
                .imageId(imageId)
                .bootVolumeSizeInGBs(volumeSize)
                .bootVolumeVpusPerGB(120).build())
            .metadata(mapOf("user_data" to """
                #!/bin/bash
                echo root:${rootPassword} |sudo chpasswd root
                sudo sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin yes/g' /etc/ssh/sshd_config;
                sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication yes/g' /etc/ssh/sshd_config;
                sudo reboot
            """.trimIndent().base64Encode()))
            .build()
        val response = computeClient(ociEntity)
            .launchInstance(LaunchInstanceRequest.builder().launchInstanceDetails(details).build())
        return response.instance
    }

    fun listVnicAttachments(ociEntity: OciEntity): List<VnicAttachment> {
        val response = computeClient(ociEntity).listVnicAttachments(
            ListVnicAttachmentsRequest.builder()
                .compartmentId(ociEntity.tenantId)
                .build()
        )
        return response.items
    }

    fun oneVnicAttachmentsByInstanceId(ociEntity: OciEntity, instanceId: String): VnicAttachment {
        val list = listVnicAttachments(ociEntity)
        return list.find { it.instanceId == instanceId } ?: throw NoSuchElementException("instanceId not found in vnicAttachment")
    }

    fun getVnic(ociEntity: OciEntity, vnicId: String): Vnic {
        val response = virtualNetworkClient(ociEntity).getVnic(
            GetVnicRequest.builder()
                .vnicId(vnicId)
                .build()
        )
        return response.vnic
    }

    fun listInstances(ociEntity: OciEntity): List<Instance> {
        val response = computeClient(ociEntity).listInstances(
            ListInstancesRequest.builder()
                .compartmentId(ociEntity.tenantId)
                .build()
        )
        return response.items
    }

    fun vnicByInstance(ociEntity: OciEntity, instance: Instance): Vnic {
        val vnicAttachment = oneVnicAttachmentsByInstanceId(ociEntity, instance.id)
        return getVnic(ociEntity, vnicAttachment.vnicId)
    }

    fun getInstance(ociEntity: OciEntity, instanceId: String): Instance {
        val response = computeClient(ociEntity).getInstance(
            GetInstanceRequest.builder()
                .instanceId(instanceId)
                .build()
        )
        return response.instance
    }

    fun listBootVolumeAttachments(ociEntity: OciEntity, instanceId: String? = null): List<BootVolumeAttachment> {
        val response = computeClient(ociEntity).listBootVolumeAttachments(
            ListBootVolumeAttachmentsRequest.builder()
                .instanceId(instanceId)
                .availabilityDomain(firstAvailabilityDomains(ociEntity).name)
                .compartmentId(ociEntity.tenantId)
                .build()
        )
        return response.items
    }

    fun getBootVolume(ociEntity: OciEntity, bootVolumeId: String): BootVolume {
        val response = blockstorageClient(ociEntity).getBootVolume(GetBootVolumeRequest.builder()
            .bootVolumeId(bootVolumeId).build())
        return response.bootVolume
    }

    fun getPublicIpByIpAddress(ociEntity: OciEntity, ip: String): PublicIp {
        val response = virtualNetworkClient(ociEntity).getPublicIpByIpAddress(GetPublicIpByIpAddressRequest.builder()
            .getPublicIpByIpAddressDetails(
                GetPublicIpByIpAddressDetails.builder()
                    .ipAddress(ip)
                    .build()
            )
            .build())
        return response.publicIp
    }

    fun updatePublicIp(ociEntity: OciEntity, ipId: String, privateIpId: String? = null): PublicIp {
        val response = virtualNetworkClient(ociEntity).updatePublicIp(UpdatePublicIpRequest.builder()
            .publicIpId(ipId)
            .updatePublicIpDetails(UpdatePublicIpDetails.builder()
                .privateIpId(privateIpId)
                .build())
            .build())
        return response.publicIp
    }

    fun deletePublicIp(ociEntity: OciEntity, ipId: String) {
        virtualNetworkClient(ociEntity).deletePublicIp(DeletePublicIpRequest.builder()
            .publicIpId(ipId)
            .build())
    }

    fun createPublicIp(ociEntity: OciEntity, lifetime: CreatePublicIpDetails.Lifetime, privateIpId: String? = null): PublicIp {
        val response = virtualNetworkClient(ociEntity).createPublicIp(CreatePublicIpRequest.builder()
            .createPublicIpDetails(CreatePublicIpDetails.builder()
                .compartmentId(ociEntity.tenantId)
                .privateIpId(privateIpId)
                .lifetime(lifetime)
                .build()
            )
            .build())
        return response.publicIp
    }

    fun listPrivateIps(ociEntity: OciEntity, ipAddress: String? = null, subnetId: String? = null, vnicId: String? = null): List<PrivateIp> {
        val response = virtualNetworkClient(ociEntity).listPrivateIps(ListPrivateIpsRequest.builder()
            .ipAddress(ipAddress)
            .subnetId(subnetId)
            .vnicId(vnicId)
            .build())
        return response.items
    }

    fun terminateInstance(ociEntity: OciEntity, instanceId: String, preserveBootVolume: Boolean = false) {
        computeClient(ociEntity).terminateInstance(TerminateInstanceRequest.builder()
            .instanceId(instanceId)
            .preserveBootVolume(preserveBootVolume)
            .build())
    }

    /**
     * STOP
     * START
     * SOFTRESET
     * RESET
     * SOFTSTOP
     * SENDDIAGNOSTICINTERRUPT
     * DIAGNOSTICREBOOT
     * REBOOTMIGRATE
     */
    fun instanceAction(ociEntity: OciEntity, instanceId: String, action: String): Instance {
        val response = computeClient(ociEntity).instanceAction(InstanceActionRequest.builder()
            .instanceId(instanceId)
            .action(action)
            .build())
        return response.instance
    }

}
