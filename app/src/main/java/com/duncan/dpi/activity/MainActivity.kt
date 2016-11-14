package com.duncan.dpi.activity


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import com.duncan.dpi.R
import com.duncan.dpi.model.Device
import com.duncan.dpi.util.CalcUtil
import com.jakewharton.rxbinding.support.design.widget.RxTextInputLayout
import com.jakewharton.rxbinding.widget.RxTextView
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent
import kotlinx.android.synthetic.main.activity_main.*
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.functions.Func1
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var width: EditText
    lateinit var height: EditText
    lateinit var screenSize: EditText
    lateinit var answer: TextView
    internal val DEVICE_LIST_REQUEST_CODE = 9999
    internal var viewedDeviceListDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val filter = Func1<TextViewTextChangeEvent, Boolean> { event -> event.text().isNotEmpty() }
        val action = Action1<TextViewTextChangeEvent> { event -> attemptCalculation() }
        RxTextView.textChangeEvents(inputWidth.editText!!)
                .debounce(700, TimeUnit.MILLISECONDS)
                .filter(filter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action)
        RxTextView.textChangeEvents(inputHeight.editText!!)
                .debounce(700, TimeUnit.MILLISECONDS)
                .filter(filter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action)
        RxTextView.textChangeEvents(inputScreenSize.editText!!)
                .debounce(700, TimeUnit.MILLISECONDS)
                .filter(filter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action)
    }

    /**
     * Attempt to calculate DPI
     */
    private fun attemptCalculation() {
        val width = try { inputWidth.editText?.text.toString().toInt() } catch (e: NumberFormatException) { 0 }
        val height = try { inputHeight.editText?.text.toString().toInt() } catch (e: NumberFormatException) { 0 }
        val screenSize = try { inputScreenSize.editText?.text.toString().toDouble() } catch (e: NumberFormatException) { 0.0 }
        labelDensity.text = "${CalcUtil.calculateDPI(width, height, screenSize)} dpi"

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val aspectRatio = width / height.toDouble()

        val maxWidth = screenWidth * 0.5
        val maxHeight = screenHeight * 0.6

        val params = imageScreen.layoutParams

        // Attempt maxWidth
        params.width = maxWidth.toInt()
        val intendedHeight = maxWidth / width * height
        if (intendedHeight > maxHeight) {
            // Use maxWidth
            val intendedWidth = maxHeight / height * width
            params.width = intendedWidth.toInt()
            params.height = maxHeight.toInt()
        } else {
            params.height = intendedHeight.toInt()
        }

        Log.d("MainA", "screen=$screenWidth x $screenHeight | image=${params.width}x${params.height}")
        imageScreen.layoutParams = params
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.deviceList -> {
                if (!viewedDeviceListDialog) {
                    val builder = AlertDialog.Builder(this@MainActivity, R.style.DialogTheme)
                    builder.setMessage(R.string.dialog_content)
                            .setTitle(R.string.device_list)
                            .setPositiveButton(R.string.dialog_agree) { dialogInterface, i ->
                                viewedDeviceListDialog = true
                                val intent = Intent(this@MainActivity, DeviceListActivity::class.java)
                                startActivityForResult(intent, DEVICE_LIST_REQUEST_CODE)
                            }
                            .setNegativeButton(R.string.dialog_cancel, null)
                    val dialog = builder.create()
                    dialog.show()
                } else {
                    //Since seen before, no need to show again.
                    val intent = Intent(this@MainActivity, DeviceListActivity::class.java)
                    startActivityForResult(intent, DEVICE_LIST_REQUEST_CODE)
                }
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEVICE_LIST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val device = data.getParcelableExtra<Device>("device")
            inputWidth.editText?.setText("${device.screenWidth}")
            inputHeight.editText?.setText("${device.screenHeight}")
            inputScreenSize.editText?.setText(String.format("%.2f", device.screenSize))
            attemptCalculation()
            Snackbar.make(imageScreen, String.format("%s %s", getString(R.string.message_populate), device.title), Snackbar.LENGTH_LONG)
                    .show()
            //Toast.makeText(MainActivity.this, "Inserted data from " + device.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }
}