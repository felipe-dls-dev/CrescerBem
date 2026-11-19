package br.com.crescerbem.ui.navigation

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.crescerbem.data.model.RecordType
import java.time.LocalDate

object Routes {
    const val REPORT = "report"
    const val SETTINGS = "settings"

    const val ARG_TYPE = "type"
    const val ARG_ID = "id"
    const val ARG_DATE = "date"
    const val ARG_PRESET = "preset"
    const val FORM_PATTERN = "form/{$ARG_TYPE}?$ARG_ID={$ARG_ID}&$ARG_DATE={$ARG_DATE}&$ARG_PRESET={$ARG_PRESET}"

    /** Rota de formulário. Sem [id] cria um registro novo; [date] e [preset] pré-preenchem. */
    fun form(type: RecordType, id: Long? = null, date: LocalDate? = null, preset: String? = null): String {
        val params = listOfNotNull(
            id?.let { "$ARG_ID=$it" },
            date?.let { "$ARG_DATE=$it" },
            preset?.let { "$ARG_PRESET=$it" },
        )
        return "form/${type.name}" + if (params.isEmpty()) "" else params.joinToString("&", prefix = "?")
    }
}

/** Destinos da barra inferior. */
enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Hoje", Icons.Filled.Today),
    CALENDAR("calendar", "Calendário", Icons.Filled.CalendarMonth),
    BABY("baby", "Bebê", Icons.Filled.ChildCare),
    HEALTH("health", "Saúde", Icons.Filled.MedicalServices),
    FEEDING("feeding", "Alimentação", Icons.Filled.Restaurant),
}

data class FormArgs(
    val type: RecordType,
    val id: Long?,
    val date: LocalDate?,
    val preset: String?,
) {
    companion object {
        fun from(bundle: Bundle?): FormArgs = FormArgs(
            type = bundle?.getString(Routes.ARG_TYPE)
                ?.let { name -> RecordType.entries.firstOrNull { it.name == name } }
                ?: RecordType.MOMENT,
            id = bundle?.getString(Routes.ARG_ID)?.toLongOrNull(),
            date = bundle?.getString(Routes.ARG_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            preset = bundle?.getString(Routes.ARG_PRESET),
        )
    }
}
