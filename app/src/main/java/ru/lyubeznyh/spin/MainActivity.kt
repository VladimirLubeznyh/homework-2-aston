package ru.lyubeznyh.spin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import ru.lyubeznyh.spin.customview.RainbowWheelView


class MainActivity : AppCompatActivity() {

    private lateinit var rainbowWheel: RainbowWheelView
    private lateinit var text: TextView
    private lateinit var seekBar: SeekBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rainbowWheel = findViewById(R.id.Spin)
        text = findViewById(R.id.textASD)
        seekBar = findViewById(R.id.seekBar)
        seekBar.progress = (rainbowWheel.ratioWheel*100).toInt()
        text.setOnClickListener { rainbowWheel.clearData() }

        seekBar.setOnSeekBarChangeListener(
            object :SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    seekBar?.let {   rainbowWheel.changingWheelSize(it.progress/100f)}
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //does nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    //does nothing
                }
            }
        )
    }
}
