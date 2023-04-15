package me.kuku.telegram.logic

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.auth.StringPrivateKeySupplier
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

}
