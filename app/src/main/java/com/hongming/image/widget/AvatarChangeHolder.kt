package com.hongming.image.widget

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.hongming.image.R

class AvatarChangeHolder(var view:View?,var key:String?=null):ImageUploadStatus {
    var pbLoading:ProgressBar?=null
    var rlUploadResult:RelativeLayout?=null
    var ivResult:ImageView?=null
    init {
        pbLoading  = view?.findViewById(R.id.pbLoading)
        rlUploadResult  = view?.findViewById(R.id.rlUploadResult)
        ivResult  = view?.findViewById(R.id.ivResult)
    }

    override fun uploadStart(key: String) {
        if (this.key?.equals(key) == true){
            pbLoading?.visibility = View.VISIBLE
            rlUploadResult?.visibility = View.GONE
        }
    }

    override fun uploadSuccess(key: String, value: String) {
        if (this.key?.equals(key) == true){
            pbLoading?.visibility = View.GONE
            rlUploadResult?.visibility = View.VISIBLE
            ivResult?.setImageResource(R.mipmap.release_icon_sussess)
            rlUploadResult?.setOnClickListener { null }
            view?.postDelayed({
                rlUploadResult?.visibility = View.GONE
            },1000)
        }
    }

    override fun uploadFailure(key: String, error: String) {
        if (this.key?.equals(key) == true){
            pbLoading?.visibility = View.GONE
            rlUploadResult?.visibility = View.VISIBLE
            ivResult?.setImageResource(R.mipmap.release_icon_failure)
            rlUploadResult?.setOnClickListener {

            }
        }
    }

    public fun clearStatus(){
        pbLoading?.visibility = View.GONE
        rlUploadResult?.visibility = View.GONE
    }
}