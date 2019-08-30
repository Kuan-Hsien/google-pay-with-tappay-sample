package com.kuanhsien.app.sample.google_pay_with_tappay_sample

import org.json.JSONException
import org.json.JSONObject

object ApiUtil {
    fun generatePayByPrimeCURLForSandBox(prime: String, partnerKey: String, merchantId: String): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("curl -X POST ")
        stringBuilder.append(AppConstants.TAPPAY_DOMAIN_SANDBOX + AppConstants.TAPPAY_PAY_BY_PRIME_URL)
        stringBuilder.append(" -H 'content-type: application/json' ")
        stringBuilder.append(" -H 'x-api-key: $partnerKey' ")
        stringBuilder.append(" -d '")

        val bodyJO = JSONObject()
        try {
            bodyJO.put("partner_key", partnerKey)
            bodyJO.put("prime", prime)
            bodyJO.put("merchant_id", merchantId)
            bodyJO.put("amount", 1)
            bodyJO.put("currency", "TWD")
            bodyJO.put("order_number", "SN0001")
            bodyJO.put("details", "item descriptions")
            val cardHolderJO = JSONObject()
            cardHolderJO.put("phone_number", "+886912345678")
            cardHolderJO.put("name", "Cardholder")
            cardHolderJO.put("email", "Cardholder@email.com")

            bodyJO.put("cardholder", cardHolderJO)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        stringBuilder.append(bodyJO.toString())
        stringBuilder.append("'")
        return stringBuilder.toString()
    }

}
