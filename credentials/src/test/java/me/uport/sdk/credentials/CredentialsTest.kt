package me.uport.sdk.credentials

import assertk.assert
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import com.uport.sdk.signer.KPSigner
import kotlinx.coroutines.runBlocking
import me.uport.sdk.core.SystemTimeProvider
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.jwt.model.JwtHeader.Companion.ES256K
import me.uport.sdk.jwt.model.JwtHeader.Companion.ES256K_R
import me.uport.sdk.testhelpers.TestTimeProvider
import org.junit.Test

class CredentialsTest {

    @Test
    fun `can normalize a known string format to a DID format`() {
        val transformations = mapOf(
                //already did
                "did:example:something something" to "did:example:something something",
                "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74" to "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#keys-1" to "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#keys-1",
                "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX" to "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX",
                "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX#owner" to "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX#owner",

                //eth addr to ethrdid
                "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74" to "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                "0XF3BEAC30c498d9e26865f34fcaa57dbb935b0d74" to "did:ethr:0xF3BEAC30c498d9e26865f34fcaa57dbb935b0d74",
                "f3beac30c498d9e26865f34fcaa57dbb935b0d74" to "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",

                //mnid to uport did
                "2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX" to "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX",
                "5A8bRWU3F7j3REx3vkJWxdjQPp4tqmxFPmab1Tr" to "did:uport:5A8bRWU3F7j3REx3vkJWxdjQPp4tqmxFPmab1Tr",

                //unknown is left intact
                "0x1234" to "0x1234",
                "2nQtiQG6Cgm1GYTBaaK" to "2nQtiQG6Cgm1GYTBaaK"
        )

        transformations.forEach { (orig, expected) ->
            assert(Credentials.normalizeKnownDID(orig)).isEqualTo(expected)
        }
    }

    @Test
    fun `signJWT uses the correct algorithm for uport did`() = runBlocking {

        val cred = Credentials("did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX", KPSigner("0x1234"))
        val jwt = cred.signJWT(emptyMap())

        val (header, _, _) = JWTTools().decode(jwt)
        assert(header.alg).isEqualTo(ES256K)

    }

    @Test
    fun `create verification test with all params`() = runBlocking {

        val expectedJWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJjbGFpbSI6eyJuYW1lIjoiSm9obiBEb2UiLCJhZ2UiOiIzNSIsImxvY2F0aW9uIjoiR2VybWFueSJ9LCJ2YyI6WyJleUpoYkdjaU9pSklVekkxTmlJc0luUjVjQ0k2SWtwWFZDSjkuZXlKemRXSWlPaUprYVdRNlpYUm9jam93ZUdZelltVmhZek13WXpRNU9HUTVaVEkyT0RZMVpqTTBabU5oWVRVM1pHSmlPVE0xWWpCa056UWlMQ0psWkhWallYUnBiMjRpT2lKTllYTjBaWEp6SWl3aWFXRjBJam94TlRFMk1qTTVNREl5ZlEud1RuUGhnTWJyU2xyV2NmUjdfX3hXYmxHLUEzbmdqTFQyYlBfTTdaOW1pWSIsImV5SmhiR2NpT2lKSVV6STFOaUlzSW5SNWNDSTZJa3BYVkNKOS5leUp6ZFdJaU9pSmthV1E2WlhSb2Nqb3dlR1l6WW1WaFl6TXdZelE1T0dRNVpUSTJPRFkxWmpNMFptTmhZVFUzWkdKaU9UTTFZakJrTnpRaUxDSnNiMk5oZEdsdmJpSTZJbFJsZUdGeklpd2lhV0YwSWpveE5URTJNak01TURJeWZRLk8yb3FZNHBnbUFtV3FlT3Q3NlBUaUIzeTlqRUdmMlphWEVoSVJlTTlJTFUiXSwiY2FsbGJhY2siOiJteWFwcDovL2dldC1iYWNrLXRvLW1lLXdpdGgtcmVzcG9uc2UudXJsIiwiaWF0IjoxMjM0NTY3OCwiZXhwIjoxMjM0ODY3OCwiaXNzIjoiZGlkOnVwb3J0OjJuUXRpUUc2Q2dtMUdZVEJhYUtBZ3I3NnVZN2lTZXhVa3FYIn0.aGy68_dqtXBi65MuDdwlVUHxJ4kBV_TjbHVKDPbyzYWyW-hCbBkO7AqLo3zN4ToiSOSZiWel4hl6p0HIBU9Hnw"

        val claim = mapOf(
                "name" to "John Doe",
                "age" to "35",
                "location" to "Germany"
        )

        val vc = listOf(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJlZHVjYXRpb24iOiJNYXN0ZXJzIiwiaWF0IjoxNTE2MjM5MDIyfQ.wTnPhgMbrSlrWcfR7__xWblG-A3ngjLT2bP_M7Z9miY",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJsb2NhdGlvbiI6IlRleGFzIiwiaWF0IjoxNTE2MjM5MDIyfQ.O2oqY4pgmAmWqeOt76PTiB3y9jEGf2ZaXEhIReM9ILU"
        )

        val timeProvider = TestTimeProvider(12345678000L)

        val cred = Credentials("did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX", KPSigner("0x1234"), timeProvider)
        val jwt = cred.createVerification(
                "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                claim,
                "myapp://get-back-to-me-with-response.url",
                vc,
                3000L
        )
        assert(jwt).isEqualTo(expectedJWT)
    }

    @Test
    fun `create verification test with required params only`() = runBlocking {

        val expectedJWT = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJjbGFpbSI6eyJuYW1lIjoiSm9obiBEb2UiLCJhZ2UiOiIzNSIsImxvY2F0aW9uIjoiR2VybWFueSJ9LCJ2YyI6W10sImNhbGxiYWNrIjoiIiwiaWF0IjoxMjM0NTY3OCwiZXhwIjoxMjM0NjI3OCwiaXNzIjoiZGlkOnVwb3J0OjJuUXRpUUc2Q2dtMUdZVEJhYUtBZ3I3NnVZN2lTZXhVa3FYIn0.C5sY_WCnSjYmqX-w3NZo9AmB6qVUy-Uwd6Fzz24CtbK0JWAYxgslqr6-JYjkB5O5Eu9IJYNS-1pKH-waNGGwmA"

        val claim = mapOf("name" to "John Doe",
                "age" to "35",
                "location" to "Germany")

        val timeProvider = TestTimeProvider(12345678000L)

        val cred = Credentials("did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX", KPSigner("0x1234"), timeProvider)
        val jwt = cred.createVerification("did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74", claim)

        assert(jwt).isEqualTo(expectedJWT)
    }

    @Test
    fun `signJWT uses the correct algorithm for non-uport did`() = runBlocking {

        val cred = Credentials("0xf3beac30c498d9e26865f34fcaa57dbb935b0d74", KPSigner("0x1234"))
        val jwt = cred.signJWT(emptyMap())

        val (header, _, _) = JWTTools().decode(jwt)
        assert(header.alg).isEqualTo(ES256K_R)

    }

    @Test
    fun `selective disclosure request contains required fields`() = runBlocking {
        val nowSeconds = Math.floor(SystemTimeProvider.nowMs() / 1000.0).toLong()
        val cred = Credentials("did:example:issuer", KPSigner("0x1234"))

        val jwt = cred.createDisclosureRequest(SelectiveDisclosureRequestParams(emptyList(), ""))
        val (_, payload, _) = JWTTools().decode(jwt)

        assert(payload.iss).isEqualTo("did:example:issuer")
        assert(payload.iat).isNotNull {
            it.isGreaterThanOrEqualTo(nowSeconds)
        }
        assert(payload.type).isEqualTo(JWTTypes.shareReq.name)
    }

    @Test
    fun `selective disclosure payload contains relevant fields`() = runBlocking {

        val params = SelectiveDisclosureRequestParams(
                requested = listOf("name", "country"),
                callbackUrl = "myapp://get-back-to-me-with-response.url",
                verified = listOf("email"),
                networkId = "0x4",
                accountType = RequestAccountType.keypair,
                vc = emptyList(),
                expiresInSeconds = 1234L,
                extras = mapOf(
                        "hello" to "world",
                        "type" to "expect this to be overwritten"
                )
        )

        val load = buildPayloadForShareReq(params)

        assert((load["requested"] as List<*>).containsAll(listOf("name", "country")))
        assert((load["verified"] as List<*>).containsAll(listOf("email")))

        assert(load["callback"]).isEqualTo("myapp://get-back-to-me-with-response.url")
        assert(load["net"]).isEqualTo("0x4")
        assert(load["act"]).isEqualTo("keypair")
        assert(load["hello"]).isEqualTo("world")
        assert(load["type"]).isEqualTo("shareReq")

        assert((load["vc"] as List<*>)).isEmpty()

    }

    @Test
    fun `personal sign request payload contains relevant fields`() = runBlocking {

        val params = PersonalSignRequestParams(
                data = "sign this message",
                callbackUrl = "myapp://get-back-to-me-with-response.url",
                from = "0x1122334455667788990011223344556677889900",
                riss = "did:ethr:0x1122334455667788990011223344556677889900",
                networkId = "0x4",
                vc = emptyList(),
                expiresInSeconds = 1234L,
                extras = mapOf(
                        "hello" to "world",
                        "type" to "expect this to be overwritten"
                )
        )

        val load = buildPayloadForPersonalSignReq(params)

        assert(load["type"]).isEqualTo("personalSigReq")
        assert(load["data"]).isEqualTo("sign this message")
        assert(load["callback"]).isEqualTo("myapp://get-back-to-me-with-response.url")
        assert(load["riss"]).isEqualTo("did:ethr:0x1122334455667788990011223344556677889900")
        assert(load["from"]).isEqualTo("0x1122334455667788990011223344556677889900")
        assert(load["net"]).isEqualTo("0x4")
        assert((load["vc"] as List<*>)).isEmpty()
        assert(load["hello"]).isEqualTo("world")

    }

    @Test
    fun `verified claim request payload contains relevant fields`() = runBlocking {

        val params = VerifiedClaimRequestParams(
                unsignedClaim = mapOf("name" to "John Doe"),
                callbackUrl = "myapp://get-back-to-me-with-response.url",
                riss = "did:ethr:0x1122334455667788990011223344556677889900",
                rexp = 1234L,
                aud = "did:ethr:0x9988776655443322110099887766554433221100",
                sub = "did:ethr:0xFFEEDDCCBBAA9988776655443322110099887766",
                issc = mapOf("dappName" to "testing"),
                vc = emptyList(),
                expiresInSeconds = 1234L,
                extras = mapOf(
                        "hello" to "world",
                        "type" to "expect this to be overwritten"
                )
        )

        val load = buildPayloadForVerifiedClaimReq(params)

        assert(load["type"]).isEqualTo("verReq")
        assert(load["unsignedClaim"]).isEqualTo(mapOf("name" to "John Doe"))
        assert(load["callback"]).isEqualTo("myapp://get-back-to-me-with-response.url")
        assert(load["riss"]).isEqualTo("did:ethr:0x1122334455667788990011223344556677889900")
        assert((load["vc"] as List<*>)).isEmpty()
        assert(load["hello"]).isEqualTo("world")
        assert(load["aud"]).isEqualTo("did:ethr:0x9988776655443322110099887766554433221100")
        assert(load["sub"]).isEqualTo("did:ethr:0xFFEEDDCCBBAA9988776655443322110099887766")
        assert(load["issc"]).isEqualTo(mapOf("dappName" to "testing"))
        assert(load["rexp"]).isEqualTo(1234L)
    }

    @Test
    fun `can return uport profile from jwt payload`() = runBlocking {

        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJoZWxsbyIsImlhdCI6MTU1NjU0MTk3OCwiZXhwIjoxNTU2NjI4Mzc4LCJhdWQiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQiLCJjYWxsYmFjayI6Imh0dHBzOi8vY2hhc3F1aS51cG9ydC5tZS9hcGkvdjEvdG9waWMvWG5IZnlldjUxeHNka0R0dSIsInR5cGUiOiJzaGFyZVJlc3AiLCJqdGkiOiJhOGNmYWE0YS1mOGUxLTQ3YmEtOTE2ZS1lZDFhNjUyN2Y1ZTUiLCJ2ZXJpZmllZCI6WyJleUpoYkdjaU9pSklVekkxTmlJc0luUjVjQ0k2SWtwWFZDSjkuZXlKemRXSWlPaUprYVdRNlpYUm9jam93ZUdZelltVmhZek13WXpRNU9HUTVaVEkyT0RZMVpqTTBabU5oWVRVM1pHSmlPVE0xWWpCa056UWlMQ0psWkhWallYUnBiMjRpT2lKTllYTjBaWEp6SWl3aWFXRjBJam94TlRFMk1qTTVNREl5ZlEud1RuUGhnTWJyU2xyV2NmUjdfX3hXYmxHLUEzbmdqTFQyYlBfTTdaOW1pWSIsImV5SmhiR2NpT2lKSVV6STFOaUlzSW5SNWNDSTZJa3BYVkNKOS5leUp6ZFdJaU9pSmthV1E2WlhSb2Nqb3dlR1l6WW1WaFl6TXdZelE1T0dRNVpUSTJPRFkxWmpNMFptTmhZVFUzWkdKaU9UTTFZakJrTnpRaUxDSnNiMk5oZEdsdmJpSTZJbFJsZUdGeklpd2lhV0YwSWpveE5URTJNak01TURJeWZRLk8yb3FZNHBnbUFtV3FlT3Q3NlBUaUIzeTlqRUdmMlphWEVoSVJlTTlJTFUiLCJleUpwYzNNaU9pSXliMlZZZFdaSVIwUndWVFV4WW1aTFFuTmFSR1IxTjBwbE9YZGxTak55TjNOV1J5SXNJbWxoZENJNk1UVXlNRE0yTmpRek1pd2ljbVZ4ZFdWemRHVmtJanBiSW01aGJXVWlMQ0p3YUc5dVpTSXNJbU52ZFc1MGNua2lMQ0poZG1GMFlYSWlYU3dpY0dWeWJXbHpjMmx2Ym5NaU9sc2libTkwYVdacFkyRjBhVzl1Y3lKZExDSmpZV3hzWW1GamF5STZJbWgwZEhCek9pOHZZMmhoYzNGMWFTNTFjRzl5ZEM1dFpTOWhjR2t2ZGpFdmRHOXdhV012V0c1SVpubGxkalV4ZUhOa2EwUjBkU0lzSW01bGRDSTZJakI0TkNJc0ltVjRjQ0k2TVRVeU1ETTJOekF6TWl3aWRIbHdaU0k2SW5Ob1lYSmxVbVZ4SW4wLkM4bVBDQ3RXbFlBbnJvZHVxeXNYWVJsNXh2ck9keDFyNGlxM0EzU21HREdadTQ3VUdUbmppWkNPck9ROEE1bFowTTlKZkRwWkRFVENLR2RKN0tVZVdRIl0sIm5ldCI6IjB4NCIsIm93biI6eyJuYW1lIjoiTWlrZSBHdW5uIiwiZW1haWwiOiJtZ3VubkB1cG9ydC5tZSJ9fQ.075oRsNEJg-BrZIWuBd2p_r1EWxVM0pqT3s6TeaFvRo"

        /*val payload = JwtPayload(
                iss = "did:ethr:0x3ff25117c0e170ca530bd5891899c183944db431",
                iat = 1556541978,
                sub = null,
                aud = "did:ethr:0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed",
                exp = 1556628378,
                callback = "https://chasqui.uport.me/api/v1/topic/XnHfyev51xsdkDtu",
                type = "shareReq",
                net = "0x4",
                act = null,
                requested = listOf("name", "phone", "country", "avatar"),
                verified = listOf(
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJlZHVjYXRpb24iOiJNYXN0ZXJzIiwiaWF0IjoxNTE2MjM5MDIyfQ.wTnPhgMbrSlrWcfR7__xWblG-A3ngjLT2bP_M7Z9miY",
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJsb2NhdGlvbiI6IlRleGFzIiwiaWF0IjoxNTE2MjM5MDIyfQ.O2oqY4pgmAmWqeOt76PTiB3y9jEGf2ZaXEhIReM9ILU",
                        "eyJpc3MiOiIyb2VYdWZIR0RwVTUxYmZLQnNaRGR1N0plOXdlSjNyN3NWRyIsImlhdCI6MTUyMDM2NjQzMiwicmVxdWVzdGVkIjpbIm5hbWUiLCJwaG9uZSIsImNvdW50cnkiLCJhdmF0YXIiXSwicGVybWlzc2lvbnMiOlsibm90aWZpY2F0aW9ucyJdLCJjYWxsYmFjayI6Imh0dHBzOi8vY2hhc3F1aS51cG9ydC5tZS9hcGkvdjEvdG9waWMvWG5IZnlldjUxeHNka0R0dSIsIm5ldCI6IjB4NCIsImV4cCI6MTUyMDM2NzAzMiwidHlwZSI6InNoYXJlUmVxIn0.C8mPCCtWlYAnroduqysXYRl5xvrOdx1r4iq3A3SmGDGZu47UGTnjiZCOrOQ8A5lZ0M9JfDpZDETCKGdJ7KUeWQ",
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJpc3MiOiIyb2VYdWZIR0RwVTUxYmZLQnNaRGR1N0plOXdlSjNyN3NWRyIsImlhdCI6MTUyMDM2NjQzMiwicmVxdWVzdGVkIjpbIm5hbWUiLCJwaG9uZSIsImNvdW50cnkiLCJhdmF0YXIiXSwicGVybWlzc2lvbnMiOlsibm90aWZpY2F0aW9ucyJdLCJjYWxsYmFjayI6Imh0dHBzOi8vY2hhc3F1aS51cG9ydC5tZS9hcGkvdjEvdG9waWMvWG5IZnlldjUxeHNka0R0dSIsIm5ldCI6IjB4NCIsImV4cCI6MTUyMDM2NzAzMiwidHlwZSI6InNoYXJlUmVxIn0.C8mPCCtWlYAnroduqysXYRl5xvrOdx1r4iq3A3SmGDGZu47UGTnjiZCOrOQ8A5lZ0M9JfDpZDETCKGdJ7KUeWQ",
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkaWQ6ZXRocjoweGE5ZTMyMzJiNjFiZGI2NzI3MTJiOWFlMzMxOTUwNjlkOGQ2NTFjMWEiLCJpYXQiOjE1NDU1Njk1NDEsImV4cCI6MTU0NjA4Nzk0MSwiYXVkIjoiZGlkOmV0aHI6MHgxMDgyMDlmNDI0N2I3ZmU2NjA1YjBmNThmOTE0NWVjMzI2OWQwMTU0Iiwic3ViIjoiIn0.Bt9Frc1QabJfpXYBoU4sns8WPeRLdKU87FncgMFq1lY"
                ),
                permissions = listOf("notifications"),
                req = null,
                nad = null,
                dad = null,
                own = mapOf(
                        "name" to "Mike Gunn",
                        "email" to "mgunn@uport.me"
                ),
                capabilities = null,
                claims = null,
                ctl = null,
                reg = null,
                rel = null,
                fct = null,
                acc = null
        )*/

        /*coEvery {
            tested.decode(eq("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJzdWIiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQiLCJjbGFpbSI6eyJuYW1lIjoiSm9obiBEb2UiLCJhZ2UiOiIzNSIsImxvY2F0aW9uIjoiR2VybWFueSJ9LCJ2YyI6W10sImNhbGxiYWNrIjoiIiwiaWF0IjoxMjM0NTY3OCwiZXhwIjoxMjM0NjI3OCwiaXNzIjoiZGlkOnVwb3J0OjJuUXRpUUc2Q2dtMUdZVEJhYUtBZ3I3NnVZN2lTZXhVa3FYIn0.C5sY_WCnSjYmqX-w3NZo9AmB6qVUy-Uwd6Fzz24CtbK0JWAYxgslqr6-JYjkB5O5Eu9IJYNS-1pKH-waNGGwmA"))
        }.returns(
                Triple(JwtHeader(alg = "ES256K-R"), payload, byteArrayOf(0, 1, 2, 3, 4))
        )*/

        val uPortProfile = Credentials("did:ethr:0x3ff25117c0e170ca530bd5891899c183944db431", KPSigner("0x1234")).verifyDisclosure(token)

        assert(uPortProfile).isNotNull()
        assert(uPortProfile?.did).isEqualTo("did:ethr:0x3ff25117c0e170ca530bd5891899c183944db431")
        assert(uPortProfile?.networkId).isEqualTo("0x4")
        assert(uPortProfile?.name).isEqualTo("Mike Gunn")
        assert(uPortProfile?.email).isEqualTo("mgunn@uport.me")
    }
}