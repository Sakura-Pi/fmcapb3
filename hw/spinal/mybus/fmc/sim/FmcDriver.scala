package mybus.fmc.sim

import spinal.core._
import spinal.core.sim._
import mybus.fmc.Fmc

case class FmcDriver(fmc: Fmc, clockDomain : ClockDomain) {

  private def n(value: Boolean): Boolean = {
    !value
  }

  def reset(): Unit = {
    fmc.A #= 0
    //fmc.D #= 0
    fmc.NOE #= n(false)
    fmc.NE  #= n(false)
    fmc.NWE #= n(false)
    clockDomain.waitSampling(1)
  }

  /**
   * Chip Select
   */
  def select(): Unit = {
    fmc.NE #= n(true)
  }

  def deselect(): Unit = {
    fmc.NE #= n(false)
  }

  def read(addr: Long, wait: Boolean): Long = {
    select()

    // RSH is bank select bit, ignore it
    // because we supposed the bank is always 0
    fmc.A #= addr >> 2

    // set read signal
    fmc.NOE #= n(true)
    fmc.NWE #= n(false)
    clockDomain.waitSampling(40)

    // wait slave to response
    if(wait) {
      clockDomain.waitSamplingWhere(fmc.NWAIT.toBoolean)
    }

    deselect()
    fmc.A #= 0
    fmc.NOE #= n(false)
    clockDomain.waitSampling(1)

    fmc.D.toLong
  }

  def write(addr: Long, data: Long, wait: Boolean): Long = {
    select()
    fmc.NWE #= n(true)
    fmc.NOE #= n(false)

    // RSH is bank select bit, ignore it
    // because we supposed the bank is always 0
    fmc.A #= addr >> 2
    fmc.D #= data
    clockDomain.waitSampling(40)

    // wait slave to response
    if(wait) {
      clockDomain.waitSamplingWhere(fmc.NWAIT.toBoolean)
    }

    deselect()
    fmc.A #= 0
    fmc.NWE #= n(false)

    fmc.D.toLong

  }
}
