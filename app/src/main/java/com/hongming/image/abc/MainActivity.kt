package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var draggablePresent: DraggablePresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //这个是图片显示的卡片
        val dragSquare: SquareDragView = findViewById(R.id.drag_square)
        //接口，传参
        //接口，传参
        draggablePresent = DraggablePresenterImpl(this, dragSquare)
        draggablePresent!!.setCustomActionDialog(MyActionDialog(this))
        //图片文件资源
        //图片文件资源
        draggablePresent!!.setImages(
            *arrayOf(
                "http://lorempixel.com/400/400?flag=0",
                "http://lorempixel.com/400/400?flag=1",
                "http://lorempixel.com/400/400?flag=2",
                "http://lorempixel.com/400/400?flag=3",
                "http://lorempixel.com/400/400?flag=4",
                "http://lorempixel.com/400/400?flag=5"
            )
        )

    }
}