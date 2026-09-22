package com.trinhcong1120.survey_service.dto.statistics;

import java.util.List;

public class AddressStatisticsResponse {

  private Long count;
  private List<AddressItem> topAddresses;
  private List<LocationItem> topProvinces;
  private List<LocationItem> topWards;

  public AddressStatisticsResponse() {
  }

  public List<AddressItem> getTopAddresses() {
    return topAddresses;
  }

  public void setTopAddresses(List<AddressItem> topAddresses) {
    this.topAddresses = topAddresses;
  }

  public List<LocationItem> getTopProvinces() {
    return topProvinces;
  }

  public void setTopProvinces(List<LocationItem> topProvinces) {
    this.topProvinces = topProvinces;
  }

  public List<LocationItem> getTopWards() {
    return topWards;
  }

  public void setTopWards(List<LocationItem> topWards) {
    this.topWards = topWards;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public static class AddressItem {

    private String province;
    private String ward;
    private Long count;

    public AddressItem() {
    }

    public AddressItem(
            String province,
            String ward,
            Long count
    ) {
      this.province = province;
      this.ward = ward;
      this.count = count;
    }

    public String getProvince() {
      return province;
    }

    public void setProvince(String province) {
      this.province = province;
    }

    public String getWard() {
      return ward;
    }

    public void setWard(String ward) {
      this.ward = ward;
    }

    public Long getCount() {
      return count;
    }

    public void setCount(Long count) {
      this.count = count;
    }
  }

  public static class LocationItem {

    private String name;
    private Long count;

    public LocationItem() {
    }

    public LocationItem(
            String name,
            Long count
    ) {
      this.name = name;
      this.count = count;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Long getCount() {
      return count;
    }

    public void setCount(Long count) {
      this.count = count;
    }
  }
}