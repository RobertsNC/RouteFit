package dal.cs.mc.routefit.helpers

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechFunctionality {
    private lateinit var textToSpeech: TextToSpeech

    //    function runs in the main thread,
    fun mainThreadInit(applicationContext: Context): TextToSpeech {
        textToSpeech = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.CANADA
            }
        })
        textToSpeech.speak(
            "Hi, Victor this is RouteFit App",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "Init"
        )
        return textToSpeech
    }

    fun speakCommands(commands: MutableList<String>) {
        textToSpeech.speak(
            commands[0], TextToSpeech.QUEUE_FLUSH,
            null, "LINE 1"
        )
    }

    fun speakLine(string: String) {
        textToSpeech.speak(
            string, TextToSpeech.QUEUE_FLUSH,
            null, "speakLineCall"
        )
    }

    fun silence() {
        textToSpeech.playSilentUtterance(15000, TextToSpeech.QUEUE_FLUSH, null)
    }
}