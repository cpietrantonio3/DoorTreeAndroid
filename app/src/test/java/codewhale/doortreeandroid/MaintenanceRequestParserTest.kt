package codewhale.doortreeandroid

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceRequestParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesNestedMaintenanceRequestsFromYearMonthDayShape() {
        val requestsRoot = json.parseToJsonElement(
            """
            {
              "2026": {
                "April": {
                  "2": {
                    "-OpEPdKHXxoUWkxbBGoD": {
                      "category": "Plumbing",
                      "createdAt": "2026-04-02T18:18:00.697Z",
                      "date": "2026-04-02",
                      "description": "Sink is leaking",
                      "internalNotes": "miii",
                      "issue": "Faucet is leaking",
                      "photos": {
                        "0": "https://example.com/a.jpg"
                      },
                      "preferredDate": "2026-04-02",
                      "priority": "High",
                      "property": "Palm Springs",
                      "status": "in progress",
                      "tenant": "Claudio Pietrantonio",
                      "unit": "2",
                      "updatedAt": "2026-04-02T18:18:00.697Z"
                    }
                  },
                  "3": {
                    "532b4876-a272-4f60-badd-4ae4d9964c1b": {
                      "category": "HVAC",
                      "createdAt": "2026-04-03T18:23:18.491828Z",
                      "date": "2026-04-03",
                      "description": "from android",
                      "internalNotes": "",
                      "issue": "from android",
                      "photos": [
                        "https://example.com/b.jpg"
                      ],
                      "preferredDate": "2026-04-03",
                      "priority": "Low",
                      "property": "Palm Springs",
                      "status": "submitted",
                      "tenant": "Claudio Pietrantonio",
                      "unit": "2",
                      "updatedAt": "2026-04-03T18:23:18.491828Z"
                    }
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val requests = MaintenanceRequestParser.parseRequestsRoot(requestsRoot)

        assertEquals(2, requests.size)
        assertEquals("532b4876-a272-4f60-badd-4ae4d9964c1b", requests.first().id)
        assertEquals(StatusBadgeStyle.Pending, requests.first().status)
        assertEquals(MaintenanceCategory.Hvac.localizedTitle, requests.first().category)
        assertEquals(MaintenancePriority.Low.localizedTitle, requests.first().priority)
        assertTrue(requests.first().internalNotes.isEmpty())
        assertEquals(1, requests.first().photos.size)
        assertEquals("-OpEPdKHXxoUWkxbBGoD", requests.last().id)
    }

    @Test
    fun parsesMaintenanceRequestsWhenDaysAreSerializedAsArray() {
        val requestsRoot = json.parseToJsonElement(
            """
            {
              "2026": {
                "April": [
                  null,
                  null,
                  {
                    "-OpEPdKHXxoUWkxbBGoD": {
                      "category": "Plumbing",
                      "createdAt": "2026-04-02T18:18:00.697Z",
                      "date": "2026-04-02",
                      "description": "Sink is leaking",
                      "internalNotes": "miii",
                      "issue": "Faucet is leaking",
                      "photos": [
                        "https://example.com/a.jpg"
                      ],
                      "preferredDate": "2026-04-02",
                      "priority": "High",
                      "property": "Palm Springs",
                      "status": "in progress",
                      "tenant": "Claudio Pietrantonio",
                      "unit": "2",
                      "updatedAt": "2026-04-02T18:18:00.697Z"
                    }
                  },
                  {
                    "532b4876-a272-4f60-badd-4ae4d9964c1b": {
                      "category": "HVAC",
                      "createdAt": "2026-04-03T18:23:18.491828Z",
                      "date": "2026-04-03",
                      "description": "from android",
                      "internalNotes": "",
                      "issue": "from android",
                      "photos": [
                        "https://example.com/b.jpg",
                        "https://example.com/c.jpg"
                      ],
                      "preferredDate": "2026-04-03",
                      "priority": "Low",
                      "property": "Palm Springs",
                      "status": "submitted",
                      "tenant": "Claudio Pietrantonio",
                      "unit": "2",
                      "updatedAt": "2026-04-03T18:23:18.491828Z"
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        ).jsonObject

        val requests = MaintenanceRequestParser.parseRequestsRoot(requestsRoot)

        assertEquals(2, requests.size)
        assertEquals("532b4876-a272-4f60-badd-4ae4d9964c1b", requests.first().id)
        assertEquals(StatusBadgeStyle.Pending, requests.first().status)
        assertEquals(MaintenanceCategory.Hvac.localizedTitle, requests.first().category)
        assertEquals(MaintenancePriority.Low.localizedTitle, requests.first().priority)
        assertEquals(2, requests.first().photos.size)
        assertEquals("-OpEPdKHXxoUWkxbBGoD", requests.last().id)
    }
}
