package bindable

import controllers.Name
import play.api.mvc.QueryStringBindable

trait NameBindable {

  implicit def nameBindable(
      implicit binder: QueryStringBindable[String]
  ): QueryStringBindable[Name] = new QueryStringBindable[Name] {
    override def bind(
        key: String,
        params: Map[String, Seq[String]]
    ): Option[Either[String, Name]] = {
      binder.bind("name", params).map(_.map(Name))

    }

    override def unbind(key: String, name: Name): String =
      binder.unbind("name", name.value)
  }
}
