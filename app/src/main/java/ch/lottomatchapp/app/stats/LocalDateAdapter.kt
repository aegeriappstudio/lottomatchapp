package ch.lottomatchapp.app.stats

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate

class LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value?.toString())
    }

    override fun read(input: JsonReader): LocalDate? {
        return if (input.peek() == null) {
            input.nextNull()
            null
        } else {
            LocalDate.parse(input.nextString())
        }
    }
}
