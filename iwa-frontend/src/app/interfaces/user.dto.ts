export interface UserProfileDto {
  name: string;
  surname: string;
  email: string;
  phoneNum: string;
  smsNotificationsEnabled: boolean;
}

export interface UserProfileUpdateDto {
  name: string;
  surname: string;
  phoneNum: string;
  smsNotificationsEnabled: boolean;
}
