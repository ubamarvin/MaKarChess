import munit.FunSuite

import makarchess.util.{Observable, Observer}

class ObserverSpec extends FunSuite:

  test("Observable.add/remove/notifyObservers call update correctly") {
    val obs = new Observable()

    var updates = 0
    val observer = new Observer:
      override def update: Unit = updates += 1

    obs.add(observer)
    obs.notifyObservers
    assertEquals(updates, 1)

    obs.remove(observer)
    obs.notifyObservers
    assertEquals(updates, 1)
  }

end ObserverSpec

