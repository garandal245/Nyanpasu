package com.zhenxiang.nyaasi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import br.tiagohm.markdownview.MarkdownView
import br.tiagohm.markdownview.css.styles.Github
import com.zhenxiang.nyaasi.api.NyaaReleasePreviewItem
import com.zhenxiang.nyaasi.view.ReleaseDataItemView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.DateFormat

class NyaaReleaseActivity : AppCompatActivity() {

    private val TAG = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nyaa_release)

        findViewById<NestedScrollView>(R.id.scroll_root).isNestedScrollingEnabled = false
        val nyaaRelease = intent.getSerializableExtra(RELEASE_INTENT_OBJ) as NyaaReleasePreviewItem?

        nyaaRelease?.let {
            val releaseTitle = findViewById<TextView>(R.id.release_title)
            releaseTitle.text = it.name

            val idView = findViewById<TextView>(R.id.release_id)
            idView.text = "ID: ${it.id}"

            val magnetBtn = findViewById<View>(R.id.magnet_btn)
            magnetBtn.setOnClickListener { _ ->
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(it.magnet))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

            val date = findViewById<ReleaseDataItemView>(R.id.release_date)
            date.setValue(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it.date))

            val seeders = findViewById<ReleaseDataItemView>(R.id.seeders)
            seeders.setValue(it.seeders.toString())

            val leechers = findViewById<ReleaseDataItemView>(R.id.leechers)
            leechers.setValue(it.leechers.toString())

            val completed = findViewById<ReleaseDataItemView>(R.id.completed)
            completed.setValue(it.completed.toString())

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val doc: Document = Jsoup.connect("https://nyaa.si/view/${it.id}").get()
                    doc.outputSettings().prettyPrint(false)
                    val descriptionMarkdown = doc.getElementById("torrent-description").html()

                    val releaseSize = doc.selectFirst("div.col-md-1:matches(File size:)").parent().select("div:matches(^\\d*\\.?\\d* [a-zA-Z]+\$)").text()

                    withContext(Dispatchers.Main) {
                        val markdownView = findViewById<MarkdownView>(R.id.release_details_markdown)
                        markdownView.addStyleSheet(Github())
                        markdownView.loadMarkdown(descriptionMarkdown)

                        val releaseSizeView = findViewById<ReleaseDataItemView>(R.id.release_size)
                        releaseSizeView.setValue(releaseSize)

                        // Hide loading circle
                        findViewById<View>(R.id.progress_frame).visibility = View.GONE
                        findViewById<View>(R.id.release_extra_data).visibility = View.VISIBLE
                        markdownView.visibility = View.VISIBLE
                    }
                } catch(e: Exception) {
                    Log.w(TAG, e)
                }
            }
        } ?: run {
            finish()
        }
    }

    companion object {
        const val RELEASE_INTENT_OBJ = "nyaaRelease"

        fun startNyaaReleaseActivity(release: NyaaReleasePreviewItem, activity: Activity) {
            val intent = Intent(activity, NyaaReleaseActivity::class.java).putExtra(RELEASE_INTENT_OBJ, release)
            activity.startActivity(intent)
        }
    }
}