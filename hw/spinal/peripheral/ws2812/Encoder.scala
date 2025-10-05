package peripheral.ws2812

import spinal.core._

case class Encoder() extends Component {
  val io = new Bundle {
    val valid  = in Bool()
    val value  = in Bool()
    val output = out Bool()
    val next   = out Bool()
  }

  // WS2812 '0'   : HIGH(0.4us) LOW(0.85us)
  // WS2812 '1'   : HIGH(0.8us) LOW(0.45us)
  // WS2812 'RST' : LOW(>=50us)
  // the top clk is running under 5MHz,
  // thus only need 2 bit cache to store the state,
  // every bit of the cache costs 0.4us to transmit

  //   +----------+----------+-------+-----------+-------+
  //   | in_valid | in_value | stage | pulse     | next  |
  //   +----------+----------+-------+-----------+-------+
  // 0 | True     | Fill     | 2     | 0         | False |
  //   +----------+----------+-------+-----------+-------+
  // 1 |          |          | 0     | 1         | False |
  //   +----------+----------+-------+-----------+-------+
  // 2 |          |          | 1     | in_value  | True  |
  //   +----------+----------+-------+-----------+-------+
  // 3 |          | Fill     | 2     | 0         | False |
  //   +----------+----------+-------+-----------+-------+

  val NEXT = Reg(Bool()) init False
  val STAGE = Reg(UInt(2 bit)) init 2
  val PULSE = Reg(Bool()) init False
  io.output := PULSE
  io.next := NEXT

  when(io.valid) {
    switch(STAGE) {
      is(2) { STAGE := 0; NEXT := False; PULSE := False }
      is(0) { STAGE := 1; NEXT := False; PULSE := True }
      is(1) { STAGE := 2; NEXT := True;  PULSE := io.value }
    }
  } otherwise {
    NEXT := False
    PULSE := False
    STAGE := 2
  }

}

