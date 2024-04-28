package com.example.videoresolution
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ApiUtils {
    fun getAndSetFarmsDropdown(context: Context, farmsDropdown: AutoCompleteTextView) {
        val farmsCall = MainActivity.RetrofitClient.instanceForGet.getFarms()

        farmsCall.enqueue(object : Callback<FarmResponse> {
            override fun onResponse(call: Call<FarmResponse>, response: Response<FarmResponse>) {
                if (response.isSuccessful) {
                    val farmResponse = response.body()

                    if (farmResponse != null) {
                        showToastCustom(context, "Fincas obtenidas.")
                        val farmsList = farmResponse.farms
                        val farmInitials =
                            farmsList.map { farm -> farm.name.split(" - ")[1] ?: "Sin Siglas" }

                        // Update the farms dropdown with the obtained initials
                        val farmAdapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            farmInitials
                        )
                        farmsDropdown.setAdapter(farmAdapter)
                    }
                } else {
                    showToastCustom(
                        context,
                        "Error al obtener fincas del servidor. Código: ${response.code()}."
                    )
                }
            }

            override fun onFailure(call: Call<FarmResponse>, t: Throwable) {
                showToastCustom(context, "Error en la solicitud de fincas: ${t.message}.")
                Log.e("GetFarms", "Error en la solicitud al servidor de fincas: ${t.message}", t)
            }
        })
    }


    fun getAndSetBlocksDropdown(context: Context, dropdown: AutoCompleteTextView) {
        val blocksCall = MainActivity.RetrofitClient.instanceForGet.getBlocks()

        blocksCall.enqueue(object : Callback<List<BlockItem>> {
            override fun onResponse(call: Call<List<BlockItem>>, response: Response<List<BlockItem>>) {
                if (response.isSuccessful) {
                    val blocksList = response.body()

                    if (blocksList != null) {
                        showToastCustom(context, "Bloques obtenidos.")
                        val blockNumbers = blocksList.map { block -> block.blockNumber }.toTypedArray()

                        val adapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            blockNumbers
                        )
                        dropdown.setAdapter(adapter)
                    }
                } else {
                    showToastCustom(context, "Error al obtener bloques del servidor. Código: ${response.code()}.")
                }
            }

            override fun onFailure(call: Call<List<BlockItem>>, t: Throwable) {
                showToastCustom(context, "Error en la solicitud de bloques: ${t.message}.")
                Log.e("GetBlocks", "Error en la solicitud al servidor de bloques: ${t.message}", t)
            }
        })
    }


    fun setBlocksDropdown(context: Context, dropdown: AutoCompleteTextView, blockNumbers: Array<String>) {
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            blockNumbers
        )
        dropdown.setAdapter(adapter)
    }


    enum class BlockRequestStatus {
        LOADING,
        SUCCESS,
        ERROR
    }

    fun getBlocks(context: Context, callback: (Array<String>?, BlockRequestStatus) -> Unit) {
        // Indicar que la petición está en curso
        callback(null, BlockRequestStatus.LOADING)

        val blocksCall = MainActivity.RetrofitClient.instanceForGet.getBlocks()

        blocksCall.enqueue(object : Callback<List<BlockItem>> {
            override fun onResponse(call: Call<List<BlockItem>>, response: Response<List<BlockItem>>) {
                if (response.isSuccessful) {
                    val blocksList = response.body()

                    if (blocksList != null) {
                        showToastCustom(context, "Bloques obtenidos.")
                        val blockNumbers = blocksList.map { block -> block.blockNumber }.toTypedArray()
                        callback(blockNumbers, BlockRequestStatus.SUCCESS)
                    }
                } else {
                    showToastCustom(context, "Error al obtener bloques del servidor. Código: ${response.code()}.")
                    callback(null, BlockRequestStatus.ERROR)
                }
            }

            override fun onFailure(call: Call<List<BlockItem>>, t: Throwable) {
                showToastCustom(context, "Error en la solicitud de bloques: ${t.message}.")
                Log.e("GetBlocks", "Error en la solicitud al servidor de bloques: ${t.message}", t)
                callback(null, BlockRequestStatus.ERROR)
            }
        })
    }


    private fun showToastCustom(context: Context, msg: String?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.custom_toast,null)

        val txtMensaje = view.findViewById<TextView>(R.id.txtMensajeToast1)
        txtMensaje.text = msg

        val toast = Toast(context)
        toast.setGravity(Gravity.CENTER_VERTICAL or Gravity.BOTTOM, 0, 200)
        toast.duration = Toast.LENGTH_LONG
        toast.view = view
        toast.show()

    }


}