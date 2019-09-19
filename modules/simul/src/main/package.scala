package lila

import lila.socket.WithSocket

package object simul extends PackageObject with WithSocket {

  private[simul] type SocketMap = lila.hub.TrouperMap[simul.Socket]

  private[simul] object RandomName {

    private val names = IndexedSeq("Actinium", "Aluminium", "Americium", "Antimony", "Argon", "Arsenic", "Astatine", "Barium", "Berkelium", "Beryllium", "Bismuth", "Bohrium", "Boron", "Bromine", "Cadmium", "Caesium", "Calcium", "Californium", "Carbon", "Cerium", "Chlorine", "Chromium", "Cobalt", "Copernicium", "Copper", "Curium", "Darmstadtium", "Dubnium", "Dysprosium", "Einsteinium", "Erbium", "Europium", "Fermium", "Flerovium", "Fluorine", "Francium", "Gadolinium", "Gallium", "Germanium", "Gold", "Hafnium", "Hassium", "Helium", "Holmium", "Hydrogen", "Indium", "Iodine", "Iridium", "Iron", "Krypton", "Lanthanum", "Lawrencium", "Lead", "Lithium", "Livermorium", "Lutetium", "Magnesium", "Manganese", "Meitnerium", "Mendelevium", "Mercury", "Molybdenum", "Moscovium", "Neodymium", "Neon", "Neptunium", "Nickel", "Nihonium", "Niobium", "Nitrogen", "Nobelium", "Oganesson", "Osmium", "Oxygen", "Palladium", "Phosphorus", "Platinum", "Plutonium", "Polonium", "Potassium", "Praseodymium", "Promethium", "Protactinium", "Radium", "Radon", "Rhenium", "Rhodium", "Roentgenium", "Rubidium", "Ruthenium", "Rutherfordium", "Samarium", "Scandium", "Seaborgium", "Selenium", "Silicon", "Silver", "Sodium", "Strontium", "Sulfur", "Tantalum", "Technetium", "Tellurium", "Tennessine", "Terbium", "Thallium", "Thorium", "Thulium", "Tin", "Titanium", "Tungsten", "Uranium", "Vanadium", "Xenon", "Ytterbium", "Yttrium", "Zinc", "Zirconium")
    private val size = names.size

    def apply(): String = names(scala.util.Random nextInt size)
  }

  private[simul] def logger = lila.log("simul")

  case class SimulTeam(id: String, name: String, isIn: Boolean)
}
