import com.coreyd97.insikt.view.logtable.LogTableColumn
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer


//Manual Tests
fun main(args: Array<String>) {
//    val filterLibrary = mock<FilterLibrary>()
//    val r = BufferedReader(InputStreamReader(System.`in`))
//    var filter: String?
//    while (!(r.readLine().also { filter = it }).isEmpty()) {
//        val res = FilterParser.parseFilter(filter, filterLibrary)
//        if(res.errors.isNotEmpty()) {
//            println(res.errors.joinToString("\n"))
//        }else{
//            println(res.expression.toString())
//        }
//        //      System.out.println(FilterParser.parseFilter(filter).expression.getFilterString());
//    }
    val col = LogTableColumn()
    col.width = 500
    println(Json { encodeDefaults = true }.encodeToString(serializer(), col) )
}

