import fmcapb4.{Config, FmcApb4Top}
import spinal.core._
import spinal.core.sim._

object MyTopLevelSim extends App {
  Config.sim.compile(FmcApb4Top()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    dut.io.fmc_slave.NE #= true
    dut.io.fmc_slave.NWE #= true
    dut.io.fmc_slave.NOE #= true
    dut.clockDomain.waitSampling(10)

    // 测试读取第一个外设 (地址 0x10000)
    println("=== 测试读取第一个外设 (0x10000) ===")
    dut.io.fmc_slave.NE #= false     // 片选有效
    dut.io.fmc_slave.NOE #= false    // 读使能有效
    dut.io.fmc_slave.A #= 0x10000L >> 2  // 设置地址
    dut.clockDomain.waitSampling(1)
    waitUntil(dut.io.fmc_slave.NWAIT.toBoolean)
    dut.clockDomain.waitSampling(5)
    dut.io.fmc_slave.NE #= true
    dut.io.fmc_slave.NOE #= true
    dut.clockDomain.waitSampling(10)

//    // 测试读取第二个外设 (地址 0x20000)
    println("=== 测试读取第二个外设 (0x20000) ===")
    dut.io.fmc_slave.NE #= false     // 片选有效
    dut.io.fmc_slave.NOE #= false    // 读使能有效
    dut.io.fmc_slave.A #= 0x20000L >> 2    // 设置地址
    dut.clockDomain.waitSampling(1)
    waitUntil(dut.io.fmc_slave.NWAIT.toBoolean)
    dut.io.fmc_slave.NE #= true
    dut.io.fmc_slave.NOE #= true
    dut.clockDomain.waitSampling(30)

  }
}
