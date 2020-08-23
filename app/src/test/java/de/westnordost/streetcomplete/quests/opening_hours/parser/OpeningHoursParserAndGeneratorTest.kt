package de.westnordost.streetcomplete.quests.opening_hours.parser

import de.westnordost.streetcomplete.quests.opening_hours.adapter.OpeningHoursRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningHoursParserAndGeneratorTest {

    @Test fun `reject invalid or unsupported syntax`() {
        reject("gibberish")

        reject("Mo-Fr 09:00-18:30 \"comment text\"") // comments
        reject("08:00-11:00 || 10:00-11:00") // with fallback

        reject("8095652114") // issue #26 in OpeningHoursParser is not crashing parser

        reject("2000 Mo 09:00-18:30") // years
        reject("2000+ Mo 09:00-18:30") // years+
        reject("2000-2044 Mo 09:00-18:30") // year ranges
        reject("2000-2044/8 Mo 09:00-18:30") // year ranges with lap years

        reject("week 01 Mo 06:00-11:30") // week indexing
        reject("week 01,03 Mo 06:00-11:30") // weeks
        reject("week 01-51 Mo 06:00-11:30") // week range indexing
        reject("week 01-51/4 Mo 06:00-11:30") // week range with lap weeks

        reject("Tu-Fr 08:00-10:00; Mo off") // off modifier
        reject("Tu-Fr 08:00-10:00; Mo unknown") // unknown modifier
        reject("Tu-Fr 08:00-10:00; Mo closed") // closed modifier

        reject("Mo-Fr") // just weekdays
        reject("PH") // just holidays

        reject("Jan-Dec") // month range, without hours range
        reject("Jan-Feb, Nov-Dec: Mo 08:30-09:00") // multiple ranges for dates
        reject("Jun+: Mo 08:30-09:00") // extended ranges for dates
        reject("Jun +Fr: Mo 08:30-09:00") // and all that other stuff...
        reject("Jun -Fr: Mo 08:30-09:00")
        reject("Jun +Fr +4 days: Mo 08:30-09:00")
        reject("Jun 5: Mo 08:30-09:00")
        reject("easter: Mo 08:30-09:00")
        reject("Jul 01-Sep 19 Th 17:30-19:30") // date range not limited to months
        reject("Jul 01-Sep 30 Th 17:30-19:30") // date range with explicit days

        reject("SH 08:00-10:00") // school holiday
        reject("PH Mo 08:00-10:00") // ph on mondays
        reject("PH +5 days 08:00-10:00") // ph with offset
        reject("PH,SH 08:00-10:00") // holiday sequence
        reject("Mo[1] 09:00-18:30") // first monday within month
        reject("Mo +2 days 10:00-12:00") // day offsets

        reject("Mo sunrise-sunset") // event based
        reject("Mo 9:00-sunset") // event based
        reject("Mo sunrise-18:00") // event based
        reject("Mo 10:00-16:00/90") // intervals

        //rules overriding earlier rules
        reject("Th 17:30-19:30; Th 20:00-22:00")
        reject("Mo-Th 17:30-19:30; Mo-Th 10:00-12:00")
        reject("PH 08:00-20:00; PH 10:00-12:00")
        reject("Mo-Th 17:30-19:30; Tu-Su 10:00-12:00")
        reject("Mo-Th 17:30-19:30; Tu 20:00-21:00")
        reject("Oct Mo 08:30-08:31;Oct Mo 18:30-18:40")
        reject("Jan-Feb Mo 08:30-08:31;Oct-Jan Mo 08:30-10:30")
        reject("Th 17:30-19:30, Th 20:00-22:00; Th 01:00-02:00")

        reject("Th 17:30-19:30; Jul-Sep Mo 17:00-19:00") // partially month based

        // looping into next day
        reject("Mo-We 20:00-02:00; Th 08:00-16:00")
    }

    @Test fun `reject rules that are currently not supported but can be reasonably added`() {
        reject("24/7")
        reject("\"some comment\"")
        reject("Mo-Fr 08:00-18:00; PH off") // PH off rule
        reject("Mar,Oct: Mo-Su 7:00-18:00") // multiple month ranges not supported
    }

    @Test fun `accepted valid and supported rule`() {
        // without explicit days of week
        accept("09:00-20:00")
        // simple weekday + time range
        accept("Mo 09:00-20:00")
        accept("Mo: 09:00-20:00", "Mo 09:00-20:00")
        accept("Mo 09:35-20:35")
        accept("Mo 00:00-24:00")
        accept("Mo 10:00-08:00")
        accept("Mo 09:00-20:00 open", "Mo 09:00-20:00")
        accept("Mo 10:00-32:00", "Mo 10:00-08:00")
        accept("Mo 5:00-9:00", "Mo 05:00-09:00")
        accept("Mo 09:00 to 20:00", "Mo 09:00-20:00")
        accept("Mo 09h00-20h00", "Mo 09:00-20:00")
        accept("Mo 09.00-20.00", "Mo 09:00-20:00")
        accept("Mo 9AM-8PM", "Mo 09:00-20:00")
        accept("mo 9h30am to 8h30pm", "Mo 09:30-20:30")
        accept("Mo 5-9", "Mo 05:00-09:00")
        accept("Mon 08:00-18:00", "Mo 08:00-18:00")
        accept("Di 08:00-18:00", "Tu 08:00-18:00")
        accept("Mo       09:00    -   20:00", "Mo 09:00-20:00")
        // time range with open end
        accept("Mo 06:00+")
        accept("Mo 06:00-06:00+", "Mo 06:00+")
        accept("Mo 06:00-18:00+")
        // multiple time ranges
        accept("Mo 08:00-12:00,13:00-14:00")
        accept("Mo 08:00-12:00,13:00-14:00,14:00-18:00")
        accept("Mo 08:00-12:00,10:00-16:00")
        // time range wrapping the day
        accept("Mo 06:00-03:00")
        // weekdays and PH
        accept("PH 08:00-10:00")
        accept("PH,Mo-Th 08:00-10:00")
        accept("Mo-Th,PH 08:00-10:00","PH,Mo-Th 08:00-10:00")
        // weekday ranges
        accept("Mo-Th 08:00-10:00")
        accept("Th-Tu 08:00-10:00")
        accept("Th    -     Tu 08:00   -   10:00", "Th-Tu 08:00-10:00")
        accept("Mo  ,   Tu   08:00  -  10:00", "Mo,Tu 08:00-10:00")
        accept("Mo-Tu 08:00-10:00", "Mo,Tu 08:00-10:00")
        // multiple weekday ranges
        accept("Mo-We,Tu 08:00-10:00", "Mo-We 08:00-10:00")
        accept("Mo,Tu,We,Th,Fr,Sa,Su 08:00-10:00", "Mo-Su 08:00-10:00")
        accept("Mo,We,Fr,Sa 08:00-10:00")
        accept("Mo,We,Fr,Sa-Su 08:00-10:00", "We,Fr-Mo 08:00-10:00")
        accept("Mo-We,Fr-Su 08:00-10:00", "Fr-We 08:00-10:00")
        accept("Mo-We,Fr-Sa 08:00-10:00", "Mo-We,Fr,Sa 08:00-10:00")
        // with date range
        accept("Jun: Th 17:30-19:30", "Jun Th 17:30-19:30")
        accept("Jun Th 17:30-19:30")
        accept("Jul-Sep Mo 17:00-19:00")
        accept("Sep-Feb Mo 17:00-19:00")
        accept("Jan-Feb Mo 17:00-19:00")
        // alltogether now
        accept("Sep-Feb PH,Mo-We,Fr,Sa 15:00-18:00,20:00-02:00+")
    }

    @Test fun `accepted valid and supported rules`() {
        accept("Mo-We 09:00-20:00; Th 08:00-16:00")
        accept("Mo 09:00-20:00; Su 10:00-11:00; Tu 09:00-20:00")
        // months
        accept("Jun Th 17:30-19:30; Jul-Sep Mo 17:00-19:00")
        accept("Jun Th 17:30-19:30; Jun Fr 10:30-20:30; Jul-Sep Mo 17:00-19:00")
        accept("Jun Th 17:30-19:30; Jun Fr 10:30-20:30; Jul-Sep Mo 17:00-19:00; Oct Mo 22:00-23:00")
        accept("Mar-Oct Tu-Su 10:30-18:00; Mar-Oct Mo 10:30-14:00; Nov-Dec Tu-Su 11:00-17:00; Nov-Dec Mo 11:00-14:00")
        // use ";" if weekdays don't collide
        accept("Su-Tu 09:00-12:00; We-Sa 10:10-10:11")
        accept("Sa-Tu 09:00-12:00; We-Fr 10:10-10:11")
        accept("Mo-We 09:00-20:00, Th 08:00-16:00", "Mo-We 09:00-20:00; Th 08:00-16:00")
        accept("Mo 09:00-20:00, Su 10:00-11:00, Tu 09:00-20:00", "Mo 09:00-20:00; Su 10:00-11:00; Tu 09:00-20:00")
        accept("Mo-Fr 07:30-18:00, Sa-Su 9:00-18:00", "Mo-Fr 07:30-18:00; Sa,Su 09:00-18:00")
        accept("Mo 17:30-19:30, Th 17:00-19:00", "Mo 17:30-19:30; Th 17:00-19:00")
        accept("Mo-Sa 07:00-20:00; PH 07:00-07:05")
        // use ";" if weekdays don't collide for months-rules
        accept("Jun Mo 17:30-19:30; Jul-Sep Mo 17:00-19:00")
        accept("Jun Mo 17:30-19:30, Jul-Sep Mo 17:00-19:00", "Jun Mo 17:30-19:30; Jul-Sep Mo 17:00-19:00")
        accept("Jun Th 17:30-19:30; Jun Fr 10:30-20:30; Jul-Sep Th 17:00-19:00")
        accept("Jun Th 17:30-19:30, Jun Fr 10:30-20:30; Jul-Sep Th 17:00-19:00", "Jun Th 17:30-19:30; Jun Fr 10:30-20:30; Jul-Sep Th 17:00-19:00")
        // use "," if weekdays collide
        accept("Mo-We 09:00-12:00, Tu 16:00-18:00")
        accept("Mo-We 09:00-20:00, We-Fr 21:00-22:00")
        accept("Mo-Sa 07:00-20:00, PH,Sa 22:00-23:00")
        accept("Mo-We 09:00-12:00, Tu 16:00-18:00; Sa 12:00-18:00", "Mo-We 09:00-12:00, Tu 16:00-18:00, Sa 12:00-18:00")
        // use "," if weekdays collide for months-rules
        accept("Jun Th 17:30-19:30, Jun-Aug Th 10:30-14:30")
        accept("Dec-Jun Th 17:30-19:30, Jun-Aug Th 10:30-14:30")
        accept("Jun Th 17:30-19:30; Jun Fr 10:30-20:30; Jul-Sep Mo 17:00-19:00, Jul-Sep Mo,Tu 22:00-23:00", "Jun Th 17:30-19:30, Jun Fr 10:30-20:30, Jul-Sep Mo 17:00-19:00, Jul-Sep Mo,Tu 22:00-23:00")
        // merging consecutive rules with same days
        accept("Mo 09:00-20:00, Mo 21:00-22:00", "Mo 09:00-20:00,21:00-22:00")
        accept("Mo-Fr 09:00-20:00, Mo-Fr 21:00-22:00", "Mo-Fr 09:00-20:00,21:00-22:00")
        accept("Mo-Fr,Su,PH 09:00-20:00, Mo-Fr,Su,PH 21:00-22:00", "PH,Su-Fr 09:00-20:00,21:00-22:00")
        accept("Jun Th 17:30-19:30, Jun Th 10:30-14:30", "Jun Th 17:30-19:30,10:30-14:30")
        accept("Jun Th 17:30-19:30, Jun Th 10:30-14:30; Jul Mo 08:30-11:00", "Jun Th 17:30-19:30,10:30-14:30; Jul Mo 08:30-11:00")
        // looping into next day
        accept("Mo-We 20:00-02:00, Th 08:00-16:00")
    }

    private fun parseAndGenerate(oh: String): String? {
        return oh.toOpeningHoursRules()?.toOpeningHoursRows()?.toOpeningHoursRules()?.toString()
    }

    private fun parse(oh: String): List<OpeningHoursRow>? {
        return oh.toOpeningHoursRules()?.toOpeningHoursRows()
    }

    private fun reject(oh: String) {
        assertNull(parse(oh))
    }

    private fun accept(oh: String, result: String = oh) {
        assertEquals(result, parseAndGenerate(oh))
    }
}
