package fmcapb4
import spinal.core.SpinalEnum

object FmcApb4State extends SpinalEnum {
  val IDLE = newElement()           // 状态0: 空闲状态
  val APB_ACCESS = newElement()     // 状态1: APB Access阶段
  val WAIT_RELEASE = newElement()   // 状态2: 等待信号释放
}
