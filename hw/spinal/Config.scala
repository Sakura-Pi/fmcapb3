package fmcapb3

import spinal.core._
import spinal.core.sim._

object Config {
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = LOW,
      clockEdge = RISING,
      resetKind = ASYNC
    ),
    onlyStdLogicVectorAtTopLevelIo = false,
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
