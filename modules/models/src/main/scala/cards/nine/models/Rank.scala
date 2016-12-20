package cards.nine.models

import cards.nine.models.types.{NineCardsCategory, NineCardsMoment}

case class PackagesByCategory(category: NineCardsCategory, packages: Seq[String])

case class PackagesByMoment(moment: NineCardsMoment, packages: Seq[String])

case class WidgetsByMoment(moment: NineCardsMoment, widgets: Seq[AppWidget])
