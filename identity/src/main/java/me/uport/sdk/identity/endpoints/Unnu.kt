package me.uport.sdk.identity.endpoints

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import me.uport.sdk.core.Networks
import me.uport.sdk.core.urlPost
import java.io.IOException


const val UNNU_URL = "https://api.uport.me/unnu"
const val identityCreationUrl = "$UNNU_URL/createIdentity"
const val identityCheckUrl = "$UNNU_URL/lookup"

/**
 * Encapsulates the payload data for an "Identity creation request" made to Unnu
 */
@Serializable
data class UnnuCreationRequest(
        @SerialName("deviceKey")
        val deviceKey: String,

        @SerialName("recoveryKey")
        val recoveryKey: String,

        @SerialName("blockchain")
        val blockchain: String,

        @SerialName("managerType")
        val managerType: String = "MetaIdentityManager") {

    fun toJson() = JSON.stringify(UnnuCreationRequest.serializer(), this)
}

/**
 * Wraps the data needed for a Unnu lookup request
 */
@Serializable
data class UnnuLookupRequest(val deviceKey: String) {

    fun toJson() = JSON.stringify(UnnuLookupRequest.serializer(), this)

}

/**
 * Encapsulates the response data for identity creation or lookup
 */
@Serializable
data class UnnuIdentityInfo(
        @SerialName("managerType")
        val managerType: String = "MetaIdentityManager",

        @SerialName("managerAddress")
        val managerAddress: String = "",

        @SerialName("txHash")
        val txHash: String? = null,

        @SerialName("identity")
        val proxyAddress: String? = null,

        @SerialName("blockchain")
        val blockchain: String? = null) {

    companion object {
        internal val blank = UnnuIdentityInfo()
    }
}

/**
 * Wraps the [UnnuIdentityInfo] response in an object suitable for receiving JsonRPC responses
 */
@Serializable
data class UnnuJRPCResponse(

        @SerialName("status")
        val status: String = "failure",

        @SerialName("message")
        val message: String? = null,

        @SerialName("data")
        val data: UnnuIdentityInfo = UnnuIdentityInfo()) {

    companion object {
        fun fromJson(json: String): UnnuJRPCResponse = JSON.nonstrict.parse(UnnuJRPCResponse.serializer(), json)
    }
}

/**
 * Queries Unnu for the address of the proxy contract created with the [deviceKeyAddress] as owner
 */
suspend fun lookupIdentityInfo(deviceKeyAddress: String): UnnuIdentityInfo {

    val rawResponse = urlPost(identityCheckUrl, UnnuLookupRequest(deviceKeyAddress).toJson(), null)
    val parsedResponse = UnnuJRPCResponse.fromJson(rawResponse)
    if (parsedResponse.status == "success") {
        return parsedResponse.data
    } else {
        throw IOException("${parsedResponse.message}")
    }
}

/**
 * Calls Unnu with the necessary params to deploy an identity proxy contract
 */
suspend fun requestIdentityCreation(deviceKeyAddress: String,
                                    recoveryAddress: String,
                                    networkId: String,
                                    fuelToken: String): UnnuIdentityInfo {

    val unnuPayload = UnnuCreationRequest(
            deviceKeyAddress,
            recoveryAddress,
            Networks.get(networkId).name).toJson()

    val rawResponse = urlPost(identityCreationUrl, unnuPayload, fuelToken)
    val parsedResponse = UnnuJRPCResponse.fromJson(rawResponse)
    if (parsedResponse.status == "success") {
        return parsedResponse.data
    } else {
        throw IOException("${parsedResponse.message}")
    }
}





