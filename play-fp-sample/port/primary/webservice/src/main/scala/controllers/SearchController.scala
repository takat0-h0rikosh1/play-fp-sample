package controllers

import java.time.{Clock, DayOfWeek, Instant, ZonedDateTime}

import javax.inject.Inject
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents
}

class SearchController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  def get(r: SearchRequest): Action[AnyContent] = Action {
    Ok(r.toString)
  }
}

/**
  * 検索条件
  * @param interval 対象期間
  * @param dayOfWeeks 曜日(月,火,水,...
  * @param note 検索文字列
  * @param limit 上限件数
  */
case class SearchRequest(
    interval: Interval,
    dayOfWeeks: Set[DayOfWeek],
    note: Option[Note],
    limit: Option[Limit]
)

case class Interval(
    from: ZonedDateTime,
    to: ZonedDateTime
)
case class Note(v: String)
case class Limit(v: Long)

object Interval {

  // long から ZonedDateTime へのパース
  def parseZDT(v: Long)(implicit c: Clock): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(v), c.getZone)

  // from, to どちらも指定があった場合に使う
  def fromTo(from: Long, to: Long)(implicit c: Clock): Interval =
    new Interval(parseZDT(from), parseZDT(to))

  // form のみ指定の場合に使う
  def fromToToday(from: Long)(implicit c: Clock): Interval =
    Interval(parseZDT(from), ZonedDateTime.now(c))

  // to のみ指定の場合に使う
  def fromUnixEpochTo(to: Long)(implicit c: Clock): Interval =
    Interval(ZonedDateTime.ofInstant(Instant.EPOCH, c.getZone), parseZDT(to))

  // どちらも指定がない場合は全期間を対象とする
  def all()(implicit c: Clock): Interval =
    Interval(ZonedDateTime.ofInstant(Instant.EPOCH, c.getZone),
             ZonedDateTime.now(c))

  def apply(from: Option[Long], to: Option[Long])(implicit c: Clock): Interval =
    (from, to) match {
      case (Some(f), Some(t)) => fromTo(f, t)
      case (Some(f), None)    => fromToToday(f)
      case (None, Some(t))    => fromUnixEpochTo(t)
      case _                  => all()
    }

}

