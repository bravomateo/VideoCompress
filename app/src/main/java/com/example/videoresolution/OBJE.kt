object BlockListSingleton {
    private val blocksList = mutableListOf<String>()

    fun getBlocksList(): MutableList<String> {
        return blocksList
    }

    fun clearBlocksList() {
        blocksList.clear()
    }

    // Opcional: Función para agregar bloques a la lista si es necesario
    fun addBlock(block: String) {
        blocksList.add(block)
    }
}
