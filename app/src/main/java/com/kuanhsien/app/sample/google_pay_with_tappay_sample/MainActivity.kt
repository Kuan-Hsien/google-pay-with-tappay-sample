package com.kuanhsien.app.sample.google_pay_with_tappay_sample

import android.Manifest.permission.READ_PHONE_STATE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import tech.cherri.tpdirect.api.*
import tech.cherri.tpdirect.callback.TPDGooglePayListener
import tech.cherri.tpdirect.callback.TPDTokenFailureCallback
import tech.cherri.tpdirect.callback.TPDTokenSuccessCallback


class MainActivity : AppCompatActivity(), View.OnClickListener, TPDTokenFailureCallback,
    TPDTokenSuccessCallback, TPDGooglePayListener {

    private val tag = this.javaClass.simpleName
    private val allowedNetworks = arrayOf(
        TPDCard.CardType.Visa,
        TPDCard.CardType.MasterCard,
        TPDCard.CardType.JCB,
        TPDCard.CardType.AmericanExpress,
        TPDCard.CardType.UnionPay
    )
    private val allowedAuthMethods = arrayOf(TPDCard.AuthMethod.PanOnly, TPDCard.AuthMethod.Cryptogram3DS)
    private lateinit var tpdGooglePay: TPDGooglePay
    private var paymentData: PaymentData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()

        Log.d(tag, "SDK version is " + TPDSetup.getVersion())

        //Setup environment.
        TPDSetup.initInstance(
            applicationContext,
            Integer.parseInt(getString(R.string.global_test_app_id)),
            getString(R.string.global_test_app_key),
            TPDServerType.Sandbox
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions()
        } else {
            prepareGooglePay()
        }
    }

    private fun setupViews() {
        tv_total_amount.text = getString(R.string.ui_total_amount)

        btn_google_payment.setOnClickListener(this)
        btn_google_payment.isEnabled = false

        btn_confirm.setOnClickListener(this)
        btn_confirm.isEnabled = false
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(applicationContext, READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(tag, "PERMISSION IS ALREADY GRANTED")
            prepareGooglePay()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(READ_PHONE_STATE), REQUEST_READ_PHONE_STATE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_PHONE_STATE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(tag, "PERMISSION_GRANTED")
                }
                prepareGooglePay()
            }
            else -> {
            }
        }
    }

    /**
     * @createPaymentDataRequest
     */
    private fun prepareGooglePay() {
        val tpdMerchant = TPDMerchant()
        tpdMerchant.supportedNetworks = allowedNetworks
        tpdMerchant.merchantName = getString(R.string.test_merchant_name)
        tpdMerchant.supportedAuthMethods = allowedAuthMethods

        val tpdConsumer = TPDConsumer()
        tpdConsumer.isPhoneNumberRequired = false
        tpdConsumer.isShippingAddressRequired = false
        tpdConsumer.isEmailRequired = false

        tpdGooglePay = TPDGooglePay(this, tpdMerchant, tpdConsumer) /** 和 Stripe 不同的地方是，需要註冊 merchant ID 並傳給 tpdGooglePay **/
        tpdGooglePay.isGooglePayAvailable(this) /** @checkGooglePay **/
    }


    // Response of isGooglePayAvailable
    override fun onReadyToPayChecked(isReadyToPay: Boolean, msg: String) {
        Log.d(tag, "Pay with Google availability : $isReadyToPay")
        if (isReadyToPay) {
            btn_google_payment.isEnabled = true
        } else {
            showMessage("Cannot use Pay with Google.")
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_google_payment) {
            tpdGooglePay.requestPayment(      /** @requestGooglePay **/
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice("1")
                    .setCurrencyCode("TWD")
                    .build(), LOAD_PAYMENT_DATA_REQUEST_CODE
            )
        } else if (view.id == R.id.btn_confirm) {
            getPrimeFromTapPay(paymentData)     /** should be called in onActivityResult **/
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE ->
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (data != null) {
                            btn_confirm.isEnabled = true
                            paymentData = PaymentData.getFromIntent(data)
                            revealPaymentInfo(paymentData)    /** show information on UI **/
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        btn_confirm.isEnabled = false
                        showMessage("Canceled by User")
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        btn_confirm.isEnabled = false
                        val status = AutoResolveHelper.getStatusFromIntent(data)
                        status?.run {
                            Log.d(tag, "AutoResolveHelper.RESULT_ERROR : ${status.statusCode}, message = ${status.statusMessage}")
                            showMessage("${status.statusCode}, message = ${status.statusMessage}")
                        } ?: Log.d(tag, "AutoResolveHelper.RESULT_ERROR : status = null")

                }
            }// Do nothing.
        }// Do nothing.
    }

    /**
     * show information on UI
     */
    private fun revealPaymentInfo(paymentData: PaymentData?) {
        paymentData?.run {
            try {
                val paymentDataJO = JSONObject(paymentData.toJson())
                val cardDescription = paymentDataJO.getJSONObject("paymentMethodData").getString("description")

                tv_buyer_Information.text = getString(R.string.ui_card_description,  cardDescription)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } ?: Log.d(tag, "paymentData is null")
    }

    /**
     * [onActivityResult]
     * Use to getPrime and send data to backend
     */
    private fun getPrimeFromTapPay(paymentData: PaymentData?) {
        showProgressDialog()
        tpdGooglePay.getPrime(paymentData, this, this)
    }

    /**
     * [onActivityResult]
     */
    override fun onSuccess(prime: String, cardInfo: TPDCardInfo) {
        hideProgressDialog()
        val resultStr = ("Your prime is $prime \n\n" +
                "Use below cURL to proceed the payment : \n" +
                ApiUtil.generatePayByPrimeCURLForSandBox(
                    prime,
                    getString(R.string.global_test_partnerKey),
                    getString(R.string.global_test_merchant_id)
                ))
        showMessage(resultStr)
        Log.d(tag, resultStr)
    }

    /**
     * [onActivityResult]
     */
    override fun onFailure(status: Int, reportMsg: String) {
        hideProgressDialog()
        showMessage("TapPay getPrime failed , status = $status, msg : $reportMsg")
        Log.d(tag, "TapPay getPrime failed : $status, msg : $reportMsg")
    }

    private fun showMessage(s: String) {
        tv_payment_result!!.text = s
    }

    private fun showProgressDialog() {
        progress_bar.isVisible = true
    }

    private fun hideProgressDialog() {
        progress_bar.isVisible = false
    }

    companion object {
        private const val REQUEST_READ_PHONE_STATE = 101
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 102
    }
}
