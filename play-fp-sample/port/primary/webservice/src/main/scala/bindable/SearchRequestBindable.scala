package bindable

import java.time.format.TextStyle
import java.time.{Clock, DayOfWeek, ZonedDateTime}
import java.util.Locale

import cats.data.EitherT
import cats.implicits._
import controllers._
import play.api.mvc.QueryStringBindable

import scala.util.Try

trait SearchRequestBindable {

  type R = SearchRequest
  type Params = Map[String, Seq[String]]
  type Bindable[A] = QueryStringBindable[A]

  implicit lazy val clock: Clock = Clock.systemDefaultZone()

  implicit def searchRequestBindable(
                                      implicit intBinder: Bindable[Int],
                                      longBinder: Bindable[Long],
                                      stringBinder: Bindable[String],
                                      stringSeqBinder: Bindable[Seq[String]],
                                      intervalBindable: Bindable[Interval],
                                      dayOfWeekBinder: Bindable[Seq[DayOfWeek]]
                                    ): Bindable[R] = new Bindable[R] {
    override def bind(key: String, params: Params): Option[Either[String, R]] = {
      implicit val p: Params = params
      for {
        interval <- EitherT(intervalBindable.bind(key, params))
        dayOfWeek <- EitherT(Option(bindToRightOption[Seq[DayOfWeek]]("dayOfWeek")))
        note = EitherT(stringBinder.bind("note", params)).valueOrF(_ => None)
        limit <- EitherT(Option(bindToRightOption[Long]("limit")))
      } yield
        SearchRequest(
          interval,
          dayOfWeek.getOrElse(Seq.empty).toSet,
          note.map(Note),
          limit.map(Limit)
        )
      }.value

    override def unbind(key: String, r: R): String = {
      def unbind[T](key: String, s: Option[T])(implicit b: Bindable[T]): Option[String] = s.map(f => b.unbind(key, f))

      val keys = Seq(
        unbind("from", Some(r.interval)),
        unbind("dayOfWeek", Some(r.dayOfWeeks.toSeq)),
        unbind("note", r.note.map(_.v)),
        unbind("limit", r.limit.map(_.v)),
      )
      keys.flatten.mkString("&")
    }

    private def bindToRightOption[A](key: String)(implicit params: Params, bindable: Bindable[A]): Either[String, Option[A]] =
      EitherT(bindable.bind(key, params)).map(Option(_)).value.getOrElse(Right(None))
  }
}

trait IntervalBindable {

  private type Bindable[A] = QueryStringBindable[A]
  private type Params = Map[String, Seq[String]]
  private type Bound[A] = Option[Either[String, A]]
  private type ZDT = ZonedDateTime

  private implicit lazy val clock: Clock = Clock.systemDefaultZone()

  implicit def intervalBinder(implicit binder: Bindable[Long]): Bindable[Interval] = new Bindable[Interval] {

    override def bind(key: String, params: Params): Bound[Interval] = Some({
      implicit val p: Params = params
      for {
        from <-  bindToRightOption[Long]("from")
        to <- bindToRightOption[Long]("to")
      } yield
        Interval(from, to)
    })

    override def unbind(key: String, value: Interval): String =
      Seq(
        binder.unbind("from", value.from.toInstant.toEpochMilli),
        binder.unbind("to", value.from.toInstant.toEpochMilli)
      ).mkString("&")

    private def bindToRightOption[A](key: String)(implicit params: Params, bindable: Bindable[A]): Either[String, Option[A]] =
      EitherT(bindable.bind(key, params)).map(Option(_)).value.getOrElse(Right(None))

  }

}

trait DayOfWeekBindable {

  private type Bindable[A] = QueryStringBindable[A]
  private type Params = Map[String, Seq[String]]
  private type Bound[A] = Option[Either[String, A]]

  implicit def dayOfWeekBinder(implicit binder: Bindable[String]): Bindable[DayOfWeek] = new Bindable[DayOfWeek] {

    override def bind(key: String, params: Params): Bound[DayOfWeek] = {
      val eitherT = for {
        str <- EitherT(binder.bind("dayOfWeek", params))
        dayOfWeek <- EitherT(Option(Try(DayOfWeek.valueOf(str.toUpperCase)).toEither.leftMap(_.getMessage)))
      } yield dayOfWeek
      eitherT.value
    }

    override def unbind(key: String, value: DayOfWeek): String =
      binder.unbind("dayOfWeek", value.getDisplayName(TextStyle.FULL, Locale.JAPAN))

  }

}

object Implicits extends SearchRequestBindable with NameBinder with IntervalBindable with DayOfWeekBindable
