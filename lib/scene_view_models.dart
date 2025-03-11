class GeoPosition {
  final String id;
  final double latitude;
  final double longitude;
  final double altitude;
  String type;

  GeoPosition({
    required this.id,
    required this.latitude,
    required this.longitude,
    required this.altitude,
    this.type = "",
  });

  Map<String, dynamic> toJson() => {
        "id": id,
        "type": type,
        "latitude": latitude,
        "longitude": longitude,
        "altitude": altitude,
      };
}
