package com.example.myapp.presentation.widget

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.example.myapp.databinding.DialogLoadingBinding

/**
 * 加载对话框
 */
class LoadingDialog(context: Context) : Dialog(context) {

    private val binding: DialogLoadingBinding

    init {
        binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        // 设置对话框属性
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setMessage(message: String) {
        binding.tvMessage.text = message
    }

    companion object {
        fun show(context: Context, message: String = "加载中..."): LoadingDialog {
            val dialog = LoadingDialog(context)
            dialog.setMessage(message)
            dialog.show()
            return dialog
        }
    }
}

