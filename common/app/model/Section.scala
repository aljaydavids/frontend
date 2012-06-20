package model

import com.gu.openplatform.contentapi.model.{ Section => ApiSection }

case class Section(private val delegate: ApiSection) extends MetaData {
  lazy val name: String = webTitle
  lazy val section: String = id

  lazy val id: String = delegate.id
  lazy val apiUrl: String = delegate.apiUrl
  lazy val webUrl: String = delegate.webUrl
  lazy val webTitle: String = delegate.webTitle

  lazy val canonicalUrl: String = webUrl

  lazy val url: String = SupportedUrl(delegate)
  lazy val linkText: String = webTitle

  override lazy val metaData: Map[String, Any] = super.metaData ++ Map(
    "keywords" -> name,
    "content-type" -> "Section"
  )
}
