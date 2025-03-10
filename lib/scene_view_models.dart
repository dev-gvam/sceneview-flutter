class GeoPosition {
  final String id;
  final double latitude;
  final double longitude;
  final double altitude;

  GeoPosition({
    required this.id,
    required this.latitude,
    required this.longitude,
    required this.altitude,
  });

  Map<String, dynamic> toJson() => {
        "id": id,
        "latitude": latitude,
        "longitude": longitude,
        "altitude": altitude,
      };
}
