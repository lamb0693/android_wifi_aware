package com.example.wifi_aware

import android.content.Context
import androidx.appcompat.app.AlertDialog

class SimpleConfirmDlg(context : Context, title : String, message : String) : AlertDialog.Builder(context){
    init{
        setTitle(title)
        setMessage(message)
        setPositiveButton("OK") { dialog, _ ->
            // Handle the OK button click here
            dialog.dismiss()
        }
    }

    fun showDialog(){
        this.create().show()
    }
}