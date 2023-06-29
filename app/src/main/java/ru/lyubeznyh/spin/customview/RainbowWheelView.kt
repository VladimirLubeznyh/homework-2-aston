package ru.lyubeznyh.spin.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import ru.lyubeznyh.spin.R
import kotlin.math.max
import kotlin.math.min


class RainbowWheelView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attrs, defStyleAttr, defStyleRes), ValueAnimator.AnimatorUpdateListener {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0, 0)

    private val colors: Array<Int> = arrayOf(
        Color.RED,
        Color.rgb(255, 165, 0),
        Color.YELLOW,
        Color.GREEN,
        Color.rgb(66, 170, 255),
        Color.BLUE,
        Color.rgb(139, 0, 255)
    )
    //This API sometimes has problems with loading images.
    // In case of an error, the line "error image"appears
    private val pathImage = "https://bing.ioliu.cn/v1/rand"

    private val paintForTriangle: Paint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            color = Color.BLACK
            strokeJoin = Paint.Join.ROUND
        }
    }

    private val paintForWheel: Paint by lazy {
        Paint().apply {
            color = colors[INDEX_RED]
            style = Paint.Style.FILL
        }
    }
    private val paintForText by lazy {
        Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
    }

    private val typedArray = context.obtainStyledAttributes(
        attrs,
        R.styleable.RainbowWheelView,
        defStyleAttr,
        defStyleRes
    )

    private var pathTriangle: Path? = null

    private val step = MAX_CIRCLE_ANGLE / colors.size
    private var startAngle = START_CIRCLE_ANGLE

    private var randomAngle: Int = MIN_CIRCLE_ANGLE.toInt()

    private var safeRectForWheel: RectF = RectF()
    private var safeRectForWheelRatio: RectF = RectF()
    private var safeRectResponse: RectF = RectF()

    //Creates an animator, sets the interpolator and animation time.
    //In the doOnEnd() method, it processes the result by calling loadImage() or setTextForIndexColor()
    private val wheelAnimator: ValueAnimator by lazy {
        ValueAnimator.ofInt(startAngle.toInt(), randomAngle).apply {
            duration = DURATION_ANIMATION
            interpolator = DecelerateInterpolator()
            addUpdateListener(this@RainbowWheelView)
            doOnEnd {
                if (startAngle > MAX_CIRCLE_ANGLE) {
                    startAngle -= (MAX_CIRCLE_ANGLE * NUMBER_COLORS)
                }
                if (getColorIndex(startAngle) % IN_HALF != 0) loadImage()
                else setTextForIndexColor(getColorIndex(startAngle))
            }
        }
    }

    private var bitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }
    private var text: String? = null
        set(value) {
            field = value
            invalidate()
        }

    //Used for bitmap scaling
    private val matrix: Matrix = Matrix()

    //A listener for glide that sets an image to a bitmap at the end of its upload
    private val requestListener: RequestListener<Bitmap> by lazy {
        object : RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>?,
                isFirstResource: Boolean
            ): Boolean {
                text = "error image"
                return true
            }

            override fun onResourceReady(
                resource: Bitmap?,
                model: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                resource?.let { bitmap = it }
                return true
            }
        }
    }

    var ratioWheel:Float = typedArray.getFloat(R.styleable.RainbowWheelView_ratioWheel, DEFAULT_RATIO_WHEEL)
        private set

    //Sets the value of the text depending on the index
    private fun setTextForIndexColor(index: Int) {
        text = when (index) {
            INDEX_RED -> "RED"
            INDEX_ORANGE -> "ORANGE"
            INDEX_YELLOW -> "YELLOW"
            INDEX_GREEN -> "GREEN"
            INDEX_LIGHT_BLUE -> "LIGHT BLUE"
            INDEX_BLUE -> "BLUE"
            INDEX_PURPLE -> "PURPLE"
            else -> "unknown"
        }
    }

    //Handles pressing so that only the wheel is pressed
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (event.y in (safeRectForWheelRatio.top..safeRectForWheelRatio.bottom)
                && event.x in (safeRectForWheelRatio.left..safeRectForWheelRatio.right)
            ) {
                performClick()
                return true
            }
        }
        return false
    }

    //Starts spinning after pressing the wheel
    override fun performClick(): Boolean {
        startSpin()
        return super.performClick()
    }

    private fun loadImage() {
        Glide.with(context)
            .asBitmap()
            .load(pathImage)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(requestListener)
            .submit()
    }

    //Calculates the wheel size depending on the coefficient from 0.0 to 1.0 and invalidate the view
    fun changingWheelSize(ratio: Float) {
        ratioWheel = ratio
        safeRectForWheelRatio.run {
            left = safeRectForWheel.centerX() - (safeRectForWheel.width() * ratio) / IN_HALF
            right = safeRectForWheel.centerX() + (safeRectForWheel.width() * ratio) / IN_HALF
            top = safeRectForWheel.centerY() - (safeRectForWheel.height() * ratio) / IN_HALF
            bottom = (safeRectForWheel.height() * ratio) / 2 + safeRectForWheel.centerY()
        }
        pathTriangle = createTrianglePath(
            safeRectForWheelRatio.centerX(),
            safeRectForWheelRatio.top,
            safeRectForWheelRatio.width()
        )
        startAngle -= NUMBER_COLORS * step
        invalidate()
    }

    //Returns the color index depending on the angle
    private fun getColorIndex(angle: Float): Int {

        val startRedAngle = START_CIRCLE_ANGLE
        val startOrangeAngle = startRedAngle + step
        val startYellowAngle = MIN_CIRCLE_ANGLE + (startOrangeAngle + step - MAX_CIRCLE_ANGLE)
        val startGreenAngle = startYellowAngle + step
        val startSianAngle = startGreenAngle + step
        val startBlueAngle = startSianAngle + step
        val startVoidAngle = startBlueAngle + step

        return when (angle) {
            in startRedAngle..startOrangeAngle -> INDEX_PURPLE
            in startOrangeAngle..MAX_CIRCLE_ANGLE -> INDEX_BLUE
            in MIN_CIRCLE_ANGLE..startYellowAngle -> INDEX_BLUE
            in startYellowAngle..startGreenAngle -> INDEX_LIGHT_BLUE
            in startGreenAngle..startSianAngle -> INDEX_GREEN
            in startSianAngle..startBlueAngle -> INDEX_YELLOW
            in startBlueAngle..startVoidAngle -> INDEX_ORANGE
            in startVoidAngle..startRedAngle -> INDEX_RED
            else -> INDEX_RED
        }
    }

    //Detects a random angle and starts the animation of the wheel rotation
    private fun startSpin() {
        if (wheelAnimator.isRunning) return
        clearData()
        randomAngle =
            (MIN_CIRCLE_ANGLE.toInt()..MAX_CIRCLE_ANGLE.toInt()).random() + (MAX_CIRCLE_ANGLE.toInt() * NUMBER_COLORS)
        wheelAnimator.repeatMode = ValueAnimator.RESTART
        wheelAnimator.setIntValues(startAngle.toInt(), randomAngle)
        wheelAnimator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom
        val defaultSize =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_SIZE,
                resources.displayMetrics
            ).toInt()
        setMeasuredDimension(
            resolveSize(max(minWidth, defaultSize), widthMeasureSpec),
            resolveSize(max(minHeight, defaultSize), heightMeasureSpec)
        )

    }

    //Subtracts the areas for drawing the wheel and the answer,
    // creates a path for the triangle and sets the wheel size depending on the ratioWheel attribute
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val safeWidth = w - paddingLeft - paddingRight
        val safeHeight = h - paddingTop - paddingBottom

        val safeLeft = paddingLeft.toFloat()
        val safeRight = safeLeft + safeWidth
        val safeTop = paddingTop.toFloat()
        val safeBottomForWill = safeTop + safeHeight / IN_HALF
        val safeBottomForResponse = safeBottomForWill + safeHeight / IN_HALF

        setSquareCenterForRect(
            safeLeft,
            safeRight,
            safeTop,
            safeBottomForWill,
            safeRectForWheel
        )
        safeRectForWheelRatio.apply {
            left = safeRectForWheel.left
            right = safeRectForWheel.right
            top = safeRectForWheel.top
            bottom = safeRectForWheel.bottom
        }
        safeRectResponse.apply {
            left = safeLeft
            right = safeRight
            top = safeBottomForWill
            bottom = safeBottomForResponse
        }
        pathTriangle = createTrianglePath(
            safeRectForWheelRatio.centerX(),
            safeRectForWheelRatio.top,
            safeRectForWheelRatio.width()
        )
        changingWheelSize(ratioWheel)
    }

    //Calculates a new safe drawing zone for the rectangle.
    // This zone is a square located in the center of the front rectangle.
    private fun setSquareCenterForRect(
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        rect: RectF
    ) {
        rect.apply {
            val height = bottom - top
            val width = right - left
            val min = min(height, width)
            val max = max(height, width)
            this.left = if (height < width) (max - min) / IN_HALF else left
            this.right = this.left + min
            this.top =
                if (height > width) paddingTop.toFloat() + (max - min) / IN_HALF else top
            this.bottom = safeRectForWheel.top + min
        }
    }

    //Creates a drawing path for a triangle
    private fun createTrianglePath(centerX: Float, top: Float, white: Float): Path =
        Path().apply {
            fillType = Path.FillType.EVEN_ODD
            moveTo(centerX, top)
            lineTo((centerX - white * RATIO_FOR_TRIANGLE), top)
            lineTo(centerX, top + (white * RATIO_FOR_TRIANGLE))
            lineTo((centerX + white * RATIO_FOR_TRIANGLE), top)
            lineTo(centerX, top)
            close()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        colors.forEach {
            paintForWheel.color = it
            canvas.drawArc(safeRectForWheelRatio, startAngle, step, true, paintForWheel)
            startAngle += step
        }
        pathTriangle?.let { canvas.drawPath(it, paintForTriangle) }

        text?.let {
            onDrawText(it, safeRectResponse.width(), safeRectResponse.height(),safeRectResponse.centerY(), canvas)
        }
        bitmap?.let {
            onDrawBitMap(it, safeRectResponse.centerX(), safeRectResponse.centerY(), canvas)
        }
    }

     //Sets the brush size depending on the minimum width or height,
     // calculates the position of the text and draws it.
    private fun onDrawText(text: String, white: Float, height:Float,centerY: Float, canvas: Canvas) {
        val fontSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            min(white,height) * RATIO_FOR_TEXT,
            resources.displayMetrics
        )
        paintForText.textSize = fontSize
        val textWidth = paintForText.measureText(text)
        canvas.drawText(text, (white - textWidth) / IN_HALF, centerY, paintForText)
    }

    //Scales the bitmap, calculates its position and draws
    private fun onDrawBitMap(bitmap: Bitmap, centerX: Float, centerY: Float, canvas: Canvas) {
        val ratio = if (bitmap.height > bitmap.width) {
            safeRectResponse.height() / bitmap.height
        } else {
            safeRectResponse.width() / bitmap.width
        }

        matrix.setScale(ratio, ratio)

        canvas.translate(
            centerX - ((bitmap.width * ratio) / IN_HALF),
            centerY - ((bitmap.height * ratio) / IN_HALF)
        )

        canvas.drawBitmap(bitmap, matrix, paintForWheel)
    }

    fun clearData() {
        bitmap = null
        text = null
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        startAngle = (animation.animatedValue as Int).toFloat()
        invalidate()
    }

    companion object {
        private const val DEFAULT_SIZE = 100f
        private const val DURATION_ANIMATION = 5000L
        private const val MAX_CIRCLE_ANGLE = 360f
        private const val MIN_CIRCLE_ANGLE = 0f
        private const val START_CIRCLE_ANGLE = 270f
        private const val NUMBER_COLORS = 7
        private const val RATIO_FOR_TRIANGLE = 0.04f
        private const val RATIO_FOR_TEXT = 0.05f
        private const val INDEX_RED = 0
        private const val INDEX_ORANGE = 1
        private const val INDEX_YELLOW = 2
        private const val INDEX_GREEN = 3
        private const val INDEX_LIGHT_BLUE = 4
        private const val INDEX_BLUE = 5
        private const val INDEX_PURPLE = 6
        private const val IN_HALF = 2
        private const val DEFAULT_RATIO_WHEEL = 1f
    }
}
