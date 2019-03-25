package me.uport.sdk.demoapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.runBlocking
import me.uport.sdk.Uport
import me.uport.sdk.core.Networks

/**
 * shows a simple call to create an account and check for an existing default
 */
class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Uport.defaultAccount == null) {
            defaultAccountView.text = "creating account, please wait"
            progressBarView.visibility = View.VISIBLE
            val acc = runBlocking { Uport.createAccount(Networks.rinkeby.networkId) }
            progressBarView.visibility = View.INVISIBLE

            defaultAccountView.text = "${acc.toJson(true)} \nHDAccount DID: ${acc.getDID()}" +
                    "\n" +
                    "HDAccount MNID: ${acc.getMnid()}"
        } else {
            defaultAccountView.text =
                    "${Uport.defaultAccount?.toJson(true)} \nHDAccount DID: ${Uport.defaultAccount?.getDID()}" +
                            "\n" +
                            "MNID: ${Uport.defaultAccount?.getMnid()}"
        }

    }
}
