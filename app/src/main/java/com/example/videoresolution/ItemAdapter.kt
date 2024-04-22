import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.example.videoresolution.R
import com.example.videoresolution.Video
import com.example.videoresolution.VideoState
import com.example.videoresolution.VideoUtils

class ItemAdapter(
    private val videos: List<Video>,
    private val onButtonClick: (Int) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {


    private val videoStates: MutableMap<Int, VideoState> = mutableMapOf()

    init {
        videos.forEachIndexed { index, _ ->
            videoStates[index] = VideoState.READY_TO_CONVERT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_video_card, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = videos[position]
        holder.farmTextView.text = "Finca: " + " " + item.farm
        holder.blockTextView.text = "Bloque: " + item.block
        holder.bedTextView.text = "Cama: " + item.bed
        holder.descriptionTextView.text = item.nameVideo

        when (videoStates[position]) {
            VideoState.READY_TO_CONVERT -> {
                holder.button.visibility = View.VISIBLE
                holder.progressBar.visibility = View.INVISIBLE
                holder.buttonSucces.visibility = View.INVISIBLE
                holder.buttonError.visibility = View.INVISIBLE

                Log.d("ItemAdapter", "State: READY_TO_CONVERT at position $position")
            }

            VideoState.UPLOADING -> {
                holder.button.visibility = View.INVISIBLE
                holder.progressBar.visibility = View.VISIBLE
                holder.buttonSucces.visibility = View.INVISIBLE
                holder.buttonError.visibility = View.INVISIBLE
                Log.d("ItemAdapter", "State: UPLOADING at position $position")
            }

            VideoState.UPLOAD_SUCCESS -> {
                holder.button.visibility = View.INVISIBLE
                holder.progressBar.visibility = View.INVISIBLE
                holder.buttonSucces.visibility = View.VISIBLE
                holder.buttonError.visibility = View.INVISIBLE
                Log.d("ItemAdapter", "State: UPLOAD_SUCCESS at position $position")
            }

            VideoState.UPLOAD_FAILED -> {
                holder.button.visibility = View.INVISIBLE
                holder.progressBar.visibility = View.INVISIBLE
                holder.buttonSucces.visibility = View.INVISIBLE
                holder.buttonError.visibility = View.VISIBLE
                Log.d("ItemAdapter", "State: UPLOAD_FAILED at position $position")
            }

            else -> {
                //holder.button.visibility = View.VISIBLE
                //holder.progressBar.visibility = View.GONE
            }
        }

        holder.button.setOnClickListener {
            onButtonClick(position)
        }
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val farmTextView: TextView = itemView.findViewById(R.id.FarmTextView)
        val blockTextView: TextView = itemView.findViewById(R.id.BlockTextView)
        val bedTextView: TextView = itemView.findViewById(R.id.BedTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val button: ImageButton = itemView.findViewById(R.id.buttonUploadVideo)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarVideo)
        val buttonSucces: ImageButton = itemView.findViewById(R.id.SuccessUploadVideo)
        val buttonError: ImageButton = itemView.findViewById(R.id.ErrorUploadVideo)
    }

    private fun updateVideoState(position: Int, state: VideoState) {
        videoStates[position] = state
        notifyDataSetChanged()
    }

    fun uploadVideo(
        position: Int,
        context: Context,
        outputFilePath: String,
        startTime: Int,
        endTime: Int,
        originalPath: String,
        width: String,
        height: String,
        fps: String
    ) {
        updateVideoState(position, VideoState.UPLOADING)

        VideoUtils.VideoConversionTaskClass(context, outputFilePath, startTime, endTime)
            .execute(
                originalPath,
                outputFilePath,
                width,
                height,
                fps
            )

        VideoUtils.getUploadVideoResultLiveData().observeForever(Observer { isSuccess ->
            if (isSuccess) {
                updateVideoState(position, VideoState.UPLOAD_SUCCESS)
            }
            else {
                updateVideoState(position, VideoState.UPLOAD_FAILED)
            }
        })

    }

}
