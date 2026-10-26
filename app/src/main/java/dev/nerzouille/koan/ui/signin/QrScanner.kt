package dev.nerzouille.koan.ui.signin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The back camera's preview, reading QR codes from its frames (zxing, off the main thread).
 * [onCode] gets each decoded text on the main thread until [accept] says it's the one; then the
 * camera closes. The camera permission must already be granted. [onError]: no usable camera.
 */
@Composable
fun QrScanner(accept: (String) -> Boolean, onCode: (String) -> Unit, onError: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentAccept by rememberUpdatedState(accept)
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnError by rememberUpdatedState(onError)
    val camera = remember { QrCamera(context) }
    var aspect by remember { mutableStateOf(3f / 4f) }
    DisposableEffect(camera) { onDispose { camera.close() } }
    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                            val size = camera.open(
                                texture,
                                accept = { currentAccept(it) },
                                onCode = { currentOnCode(it) },
                                onError = { currentOnError() },
                            ) ?: return
                            // The sensor is landscape: in portrait the preview is as tall as it is wide.
                            aspect = size.height.toFloat() / size.width
                        }

                        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit
                        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                            camera.close()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                    }
                }
            },
            // Fills [modifier]'s width, cropped above and below when that box is shorter.
            modifier = Modifier.fillMaxWidth().wrapContentHeight(unbounded = true).aspectRatio(aspect),
        )
    }
}

/** Camera2 plumbing: one back camera, a preview, and a small YUV stream for decoding. */
private class QrCamera(context: Context) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var thread: HandlerThread? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private val done = AtomicBoolean(false)
    private val decoding = AtomicBoolean(false)
    private val qr = QRCodeReader()
    private val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)

    /** Starts the camera into [texture]; returns the preview size, or null (and [onError]) without a camera. */
    @SuppressLint("MissingPermission")
    fun open(texture: SurfaceTexture, accept: (String) -> Boolean, onCode: (String) -> Unit, onError: () -> Unit): Size? {
        val id = runCatching {
            manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
                ?: manager.cameraIdList.firstOrNull()
        }.getOrNull()
        if (id == null) {
            onError()
            return null
        }
        val map = manager.getCameraCharacteristics(id).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val size = map?.getOutputSizes(ImageFormat.YUV_420_888)?.let(::pick) ?: Size(1280, 720)
        texture.setDefaultBufferSize(size.width, size.height)
        val worker = HandlerThread("KoanQr").also { it.start() }
        thread = worker
        val handler = Handler(worker.looper)
        val images = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
        reader = images
        images.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (done.get() || !decoding.compareAndSet(false, true)) return@setOnImageAvailableListener
                val plane = image.planes[0]
                val buffer = plane.buffer
                val data = ByteArray(buffer.remaining()).also(buffer::get)
                // The Y plane alone is a grey image; rows are rowStride apart. No code in view = no text.
                val text = runCatching {
                    val source = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
                    qr.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
                }.getOrNull()
                decoding.set(false)
                if (text != null) {
                    main.post {
                        if (done.get()) return@post
                        if (accept(text)) {
                            done.set(true)
                            close()
                        }
                        onCode(text)
                    }
                }
            } finally {
                image.close()
            }
        }, handler)
        val preview = Surface(texture)
        runCatching {
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    device = camera
                    @Suppress("DEPRECATION")
                    camera.createCaptureSession(listOf(preview, images.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            session = s
                            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(preview)
                                addTarget(images.surface)
                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            }.build()
                            runCatching { s.setRepeatingRequest(request, null, handler) }
                        }

                        override fun onConfigureFailed(s: CameraCaptureSession) {
                            main.post(onError)
                        }
                    }, handler)
                }

                override fun onDisconnected(camera: CameraDevice) = camera.close()
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    main.post(onError)
                }
            }, handler)
        }.onFailure {
            onError()
            return null
        }
        return size
    }

    fun close() {
        runCatching { session?.close() }
        runCatching { device?.close() }
        runCatching { reader?.close() }
        session = null
        device = null
        reader = null
        thread?.quitSafely()
        thread = null
    }

    /** A size near 1280×720: sharp enough for a code on a screen, light enough to decode quickly. */
    private fun pick(sizes: Array<Size>): Size? =
        sizes.filter { it.width <= 1920 && it.height <= 1080 }.minByOrNull { kotlin.math.abs(it.width * it.height - 1280 * 720) }
}
