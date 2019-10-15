package core

import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.routing.Router
import router.Routes

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application =
    new AppComponents(context).application
}

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with Components
    with play.filters.HttpFiltersComponents {

  lazy val router: Router = {
    val prefix: String = "/"
    wire[Routes]
  }
}
