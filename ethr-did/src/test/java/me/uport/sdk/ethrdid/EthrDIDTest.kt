@file:Suppress("UNCHECKED_CAST", "unused", "MemberVisibilityCanBePrivate")

package me.uport.sdk.ethrdid

import assertk.assert
import assertk.assertions.isEqualTo
import com.uport.sdk.signer.KPSigner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import me.uport.sdk.jsonrpc.JsonRPC
import org.junit.Test
import org.walleth.khex.prepend0xPrefix
import java.math.BigInteger

class EthrDIDTest {

    val originalPrivKey = "278a5de700e29faae8e40e366ec5012b5ec63d36ec77e8a2417154cc1d25383f"
    val originalAddress = "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"

    val newPrivKey = "5047c789919e943c559d8c134091d47b4642122ba0111dfa842ef6edefb48f38"
    val newAddress = "0x45c4EBd7Ffb86891BA6f9F68452F9F0815AAcD8b"

    val rinkebyRegistry = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b"

    @Test
    fun `lookup owner works for new identity`() {

        val rpc = mockk<JsonRPC>()

        //language=json
        coEvery { rpc.ethCall(any(), any()) } returns """{"result":"0x0000000000000000000000001122334455667788990011223344556677889900"}"""

        val ethrDid = EthrDID("0x11", rpc, rinkebyRegistry, KPSigner(originalPrivKey))
        val owner = runBlocking {
            ethrDid.lookupOwner()
        }
        assert(owner).isEqualTo("0x1122334455667788990011223344556677889900")
    }

    @Test
    fun `change owner sends proper transaction`() {

        runBlocking {

            val signer = KPSigner(originalPrivKey)
            val address = signer.getAddress().prepend0xPrefix()

            val rpc = mockk<JsonRPC>()

            coEvery { rpc.getTransactionCount(any()) } returns BigInteger.ZERO
            coEvery { rpc.getGasPrice() } returns 20_000_000_000L.toBigInteger()
            //language=json
            coEvery { rpc.ethCall(any(), any()) } returns """{"result":"0x0000000000000000000000001122334455667788990011223344556677889900"}"""

            coEvery { rpc.sendRawTransaction(any()) } returns "mockedTxHash"

            val ethrDid = EthrDID(address, rpc, rinkebyRegistry, signer)

            val txHash = ethrDid.changeOwner(newAddress)

            assert(txHash).isEqualTo("mockedTxHash")

            val expectedSignedTx = "0xf8aa808504a817c8008301117094dca7ef03e98e0dc2b855be647c39abe984fcf21b80b844f00d4b5d000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d7400000000000000000000000045c4ebd7ffb86891ba6f9f68452f9f0815aacd8b1ca0eb687cc4a323d4c3471d01d3a0d3d212754539fa9d2f6973acc0f1de275f53e9a0257684845b8d3d5e0c0838c5da007ddc7a0df08722fba53866601821f0aceff4"

            coVerify {
                rpc.sendRawTransaction(eq(expectedSignedTx))
            }

        }
    }
}