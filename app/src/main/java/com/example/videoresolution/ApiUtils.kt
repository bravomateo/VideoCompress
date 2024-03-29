package com.example.videoresolution
import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
                        showToast(context, "Fincas obtenidas.")
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
                    showToast(
                        context,
                        "Error al obtener fincas del servidor. Código: ${response.code()}"
                    )
                }
            }

            override fun onFailure(call: Call<FarmResponse>, t: Throwable) {
                showToast(context, "Error en la solicitud de fincas: ${t.message}")
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
                        showToast(context, "Bloques obtenidos.")
                        val blockNumbers = blocksList.map { block -> block.blockNumber }.toTypedArray()
                        val adapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            blockNumbers
                        )
                        dropdown.setAdapter(adapter)
                    }
                } else {
                    showToast(context, "Error al obtener bloques del servidor. Código: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<BlockItem>>, t: Throwable) {
                showToast(context, "Error en la solicitud de bloques: ${t.message}")
                Log.e("GetBlocks", "Error en la solicitud al servidor de bloques: ${t.message}", t)
            }
        })
    }
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}