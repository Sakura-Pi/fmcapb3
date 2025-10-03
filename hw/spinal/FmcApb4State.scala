package fmcapb4
import spinal.core._

object FmcApb4State extends SpinalEnum {
  val IDLE = newElement()           // 状态0: 空闲状态
  val APB_ACCESS = newElement()     // 状态1: APB Access阶段
  val WAIT_RELEASE = newElement()   // 状态2: 等待信号释放
  
  // 添加编码约束以确保状态值的稳定性
  //  defaultEncoding = SpinalEnumEncoding("binary")
}
