package com.wiva.android.data.remote.nanokassa

import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

internal object NanoKassaEncryptionHelper {
    private const val HMAC_FIRST_B64 = "BBuXaXBdHg+wLPjRJpf3N/NmLq5kuvzGQx3II15/j8o="
    private const val HMAC_SECOND_B64 = "aFZP3PbvrMZNNxxqJxaCnCLama5L8H1/YGO3UYsoCVQ="

    private const val RSA_PUB_FIRST =
        "<RSAKeyValue><Modulus>wFXHnzc5YKj8e3tlNzSTCkA8Tq4gjTH0VMuhJhg5QWpFjFKwtnK3u4EOaQGmjqDtzyffVHmKuGikg9jE" +
            "20sGnJN4hTtySihOiUWRd4zhJVMevBQmsEQS33bg26UzzKCeO12mbM/Q4ip7YXEfWM/FTq2l94psQgmIDh/LtHVf3OBlz8I6u5Va" +
            "P3AS0Hv9RBUin0RBkRUC+5tgURm382XTnJ2GzZ8cEGJm3C+s0+W1N2igjV0X3MihylHGDyl+8FpbFIlXsaJOYQ0//JIgnaBzMV2" +
            "JyNTHBzPJrcIMHIbKBVAmDLfgeDNKug7wIadEcqoJaCz74yG9l9nJWISWQkI6Ed8nDVsoaIkMQBuWWxfHjQEU8R8OVjRzhOGHPG2" +
            "ka6y1/jcOS5JWPzS5YVXRPbrhQYcoNebsOBaFxJYZ2E7VhVdrGWlBqhANFba7umZXVOvmDXIsH974Yv4awAaP70VPSLFIdjiNy/S" +
            "B8w0O8PJOUPznpMhvi1clBgp3PvtYmhUqmdHWPwjcjy0JmY9KrWz00Im1yDTTybtV3uYnwR677TmsLmR9c6T7EHlT3gG6Y0bM3w9" +
            "tyrGqVKy1jIkyUZPVf0dmXTfbh+hcC5kYal+M7lcn7wSSLHTUk+C/YWE1e5TvTBK6teU2VNmz80Yt2IS2mcXlfKlZXilMmPJCdUI" +
            "7nNM=</Modulus><Exponent>AQAB</Exponent></RSAKeyValue>"

    private const val RSA_PUB_SECOND =
        "<RSAKeyValue><Modulus>+fu+NGlnWAXqIVgEL37veatlyooYi+iHLiBmCDowNZUBAiQ+pvbnzkowUKdr86lGrzQLCAvVyXWG0U4kdix" +
            "AX0GTkIR/3g3h2/8hRx0x3K0umT+tcZC3iJytKzP+EM/B6sDdw6/URbykwvrAlbQsG9d6eCqq0F/6muOM3gQazy8CuHyx4iFQpm" +
            "l4E1/IQgp3tZJOX5I9xieHTUwct2OkURCKYnHJZrRIN9rwXQkNG1q+M8HDqI1Mwq88wieVC+SUuoPc8F0MlIWs2zwDhLcX84OQTR" +
            "FqlW3NFR/6kUn3TIC1JZD1Ft/8fWukZzAFsAmdXmFzhBUuBPvjIzzLafY3f8IszADMnloJ0BW3iGVRGj6hygX7Jpr/86LPHu6PBJ" +
            "zHzCp9bnfOiSjRENzzy55fDdVbYpVgWDt4+UEkl9qNRNuiSMDpKeVNy6jxbihZneYCR8alnH8Olh6lL7bmGdwwqI9LSyq/qFfIMD" +
            "V8onit/dLxzypFJofRfjZ1Dc8ZEqh2sab8qEMNPGQwTM/FVFWMbq0hmjjY+BFWGY/h0z1NZMX75Uzyd9OdXaRoTlHPfOxxAIfclP" +
            "2XY2K8f5PQ37g/fX2R8bw/fXQd2ndi/+uPCGK92Xw4/3/osJKpm3QSYhSda53T9Ddned7BtWDQJqdVY/SUskwLLyjtSb0LqsSKBH" +
            "k=</Modulus><Exponent>AQAB</Exponent></RSAKeyValue>"

    private val secureRandom = SecureRandom()

    fun buildEncryptedBody(
        requestJson: String,
        kassaId: String,
        kassaToken: String,
        isTest: Boolean = false,
    ): NanoKassaEncryptedRequest {
        val (ab, de) = encryptLayer(requestJson, HMAC_FIRST_B64, RSA_PUB_FIRST)
        val encr1 =
            NanoKassaEncr1(
                ab = ab,
                de = de,
                kassaid = kassaId,
                kassatoken = kassaToken,
                test = if (isTest) 1 else 0,
            )
        val encr1Json = NanoKassaJson.encodeToString(NanoKassaEncr1.serializer(), encr1)
        val (aab, dde) = encryptLayer(encr1Json, HMAC_SECOND_B64, RSA_PUB_SECOND)
        return NanoKassaEncryptedRequest(aab = aab, dde = dde, test = if (isTest) 1 else 0)
    }

    private fun encryptLayer(
        plainJson: String,
        hmacKeyBase64: String,
        rsaXml: String,
    ): Pair<String, String> {
        val iv = ByteArray(16).also { secureRandom.nextBytes(it) }
        val pw = ByteArray(32).also { secureRandom.nextBytes(it) }
        val hmacKey = hmacKeyBase64.decodeBase64()!!.toByteArray()

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(pw, "AES"), IvParameterSpec(iv))
        val dataAES = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(hmacKey, "HmacSHA512"))
        mac.update(iv)
        mac.update(dataAES)
        val hmac = mac.doFinal()

        val de = hmac + iv + dataAES

        val publicKey = parseRsaXmlPublicKey(rsaXml)
        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val ab = rsaCipher.doFinal(pw)

        return Pair(
            ab.toByteString(0, ab.size).base64(),
            de.toByteString(0, de.size).base64(),
        )
    }

    private fun parseRsaXmlPublicKey(xml: String): java.security.PublicKey {
        val modulusB64 = xml.substringAfter("<Modulus>").substringBefore("</Modulus>")
        val exponentB64 = xml.substringAfter("<Exponent>").substringBefore("</Exponent>")
        val modulus = BigInteger(1, modulusB64.decodeBase64()!!.toByteArray())
        val exponent = BigInteger(1, exponentB64.decodeBase64()!!.toByteArray())
        return KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))
    }
}
