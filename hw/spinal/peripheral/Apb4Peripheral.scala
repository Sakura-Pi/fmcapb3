package peripheral

import mybus.Apb4Bus
import spinal.core.Component

trait Apb4Peripheral extends Component {
  def getApb4Interface: Apb4Bus
}
