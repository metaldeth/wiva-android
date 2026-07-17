package com.wiva.android.data.remote.nanokassa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val NanoKassaJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class NanoKassaProductDto(
    @SerialName("name_tovar") val nameTavar: String,
    @SerialName("price_piece_bez_skidki") val pricePieceBezSkidki: Int,
    @SerialName("skidka") val skidka: Int = 0,
    @SerialName("kolvo") val kolvo: Int,
    @SerialName("price_piece") val pricePiece: Int,
    @SerialName("summa") val summa: Int,
    @SerialName("stavka_nds") val stavkaNds: Int = 6,
    @SerialName("priznak_sposoba_rascheta") val prizakSosobaRascheta: Int = 4,
    @SerialName("priznak_predmeta_rascheta") val prizakPredmetaRascheta: Int = 1,
    @SerialName("priznak_agenta") val prizakAgenta: String = "none",
)

@Serializable
internal data class NanoKassaOplataDto(
    @SerialName("rezhim_nalog") val rezhimNalog: String = "2",
    @SerialName("money_nal") val moneyNal: Int,
    @SerialName("money_electro") val moneyElectro: Int,
    @SerialName("money_predoplata") val moneyPredoplata: Int = 0,
    @SerialName("money_postoplata") val moneyPostoplata: Int = 0,
    @SerialName("money_vstrecha") val moneyVstrecha: Int = 0,
    @SerialName("client_email") val clientEmail: String = "g@mail.ru",
    @SerialName("client_phone") val clientPhone: String = "79920008888",
)

@Serializable
internal data class NanoKassaItogDto(
    @SerialName("priznak_rascheta") val priznakRascheta: Int = 1,
    @SerialName("itog_cheka") val itogCheka: Int,
)

@Serializable
internal data class NanoKassaRequestBody(
    @SerialName("kassaid") val kassaid: String,
    @SerialName("kassatoken") val kassatoken: String,
    @SerialName("cms") val cms: String = "wordpress",
    @SerialName("check_send_type") val checkSendType: String = "email",
    @SerialName("name_zakaz") val nameZakaz: String,
    @SerialName("check_vend_address") val checkVendAddress: String,
    @SerialName("check_vend_mesto") val checkVendMesto: String,
    @SerialName("check_vend_num_avtovat") val checkVendNumAvtovat: String,
    @SerialName("products_arr") val productsArr: List<NanoKassaProductDto>,
    @SerialName("oplata_arr") val oplataArr: NanoKassaOplataDto,
    @SerialName("itog_arr") val itogArr: NanoKassaItogDto,
)

@Serializable
internal data class NanoKassaEncr1(
    @SerialName("ab") val ab: String,
    @SerialName("de") val de: String,
    @SerialName("kassaid") val kassaid: String,
    @SerialName("kassatoken") val kassatoken: String,
    @SerialName("test") val test: Int,
)

@Serializable
internal data class NanoKassaEncryptedRequest(
    @SerialName("aab") val aab: String,
    @SerialName("dde") val dde: String,
    @SerialName("test") val test: Int,
)

@Serializable
internal data class NanoKassaServerResponse(
    @SerialName("code") val code: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("success") val success: String = "",
    @SerialName("error") val error: String = "",
    @SerialName("nuid") val nuid: String = "",
    @SerialName("qnuid") val qnuid: String = "",
)
