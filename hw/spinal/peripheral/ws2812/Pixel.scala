package peripheral.ws2812
import spinal.core._

case class RgbData() extends Bundle {
  val RED   = UInt(8 bits)
  val GREEN = UInt(8 bits)
  val BLUE  = UInt(8 bits)
//  val _R = UInt(8 bits)
}
