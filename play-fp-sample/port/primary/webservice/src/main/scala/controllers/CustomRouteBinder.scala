package controllers

import play.api.mvc.QueryStringBindable

object CustomRouteBinder {
  implicit def queryStringBindable(
    implicit intBinder: QueryStringBindable[Int],
    setStrBinder: QueryStringBindable[Seq[String]]
  ): QueryStringBindable[AgeRange] = new QueryStringBindable[AgeRange] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AgeRange]] = {
      for {
        from <- intBinder.bind("from", params)
        to <- intBinder.bind("to", params)
        setStr = setStrBinder.bind("setStr", params).getOrElse(Right(Seq.empty[String]))
      } yield {
        (from, to, setStr) match {
          case (Right(from), Right(to), Right(setStr)) => Right(AgeRange(from, to, setStr.toSet))
          case _ => Left("Unable to bind an AgeRange")
        }
      }
    }

    override def unbind(key: String, ageRange: AgeRange): String = {
      intBinder.unbind("from", ageRange.from) + "&" + intBinder.unbind("to", ageRange.to)
    }
  }
}

case class AgeRange(from: Int, to: Int, setStr: Set[String])
