package uk.co.jakelee.apodwallpaper.config

class Config: IConfig {
    override fun getUrl(auth: String, date: String) =
        "https://api.nasa.gov/planetary/apod?api_key=$auth&date=$date&hd=true"
}