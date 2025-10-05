package peripheral.ws2812

import spinal.core._
import spinal.lib._

case class Channel() extends Component {
  val io = new Bundle {
    val data   = slave Stream RgbData()
    val output = out Bool()
  }

  //   +-----------+---------+---------+---------+---------+--------------+---------+
  //   | in_enable | in_data | refill  | cache   | counter | s_value      | s_valid |
  //   +-----------+---------+---------+---------+---------+--------------+---------+
  // 0 | True      | fired   | True    |         |         |              |         |
  //   +-----------+---------+---------+---------+---------+--------------+---------+
  // 1 |           |         | False   | read    | 0       |              | True    |
  //   +-----------+---------+---------+---------+---------+--------------+---------+
  // 2 |           |         |         |         | 1       | w cache(0)   |         | when(s_next) counter := counter + 1
  //   +-----------+---------+---------+---------+---------+--------------+---------+
  // 3 |           |         |         |         | 2       | w cache(1)   |         |
  //   +-----------+---------+---------+---------+---------+--------------+---------+
  // 4 |           |         | True    |         | n       | w cache(n-1) |         |
  //   +-----------+---------+---------+---------+---------+--------------+---------+
  // 5 |           |         |         |         |         | w cache (n)  | False   | when(counter == width) stage := 1
  //   +-----------+---------+---------+---------+---------+--------------+---------+

  val CACHE     = Reg(Bits(24 bit)) init 0
  val COUNTER   = Reg(UInt(5  bit)) init 0
  val REFILL    = Reg(Bool()) init True
  val ENCODER   = Encoder()
  val ENC_NEXT  = Reg(Bool()) init False
  val ENC_VALUE = Reg(Bool()) init False
  val ENC_VALID = Reg(Bool()) init False
  io.output := ENCODER.io.output
  io.data.ready := REFILL
  ENCODER.io.valid := ENC_VALID
  ENCODER.io.value := ENC_VALUE
  ENC_NEXT := ENCODER.io.next

  // update the cache when stream was fired
  when(io.data.fire && !ENC_VALID) {
    REFILL := False
    CACHE := io.data.payload.asBits
    COUNTER := 0
    ENC_VALID := True
  }

  when(ENC_VALID) {
    when(COUNTER === 0) {
      COUNTER := COUNTER + 1
    }
    when(COUNTER < CACHE.getWidth) {
      REFILL.setWhen(COUNTER === CACHE.getWidth - 1)
    } elsewhen(COUNTER === CACHE.getWidth) {
      ENC_VALID := False
    }
    when(ENC_NEXT) {
      ENC_VALUE := CACHE(COUNTER)
      COUNTER := COUNTER + 1
    }
  }
}