/**
 * Copyright 2011 Kurtis L. Nusbaum
 * 
 * This file is part of UDJ.
 * 
 * UDJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * UDJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with UDJ.  If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef COMM_ERROR_HANDLER_HPP
#define COMM_ERROR_HANDLER_HPP

#include "ConfigDefs.hpp"
#include <QObject>
#include <QByteArray>

namespace UDJ{

class UDJServerConnection;
class DataStore;

class CommErrorHandler : public QObject{
Q_OBJECT
public:

  enum CommErrorType{
    AUTH
  };


  CommErrorHandler(
    DataStore *dataStore, 
    UDJServerConnection *serverConnection);

signals:

  void hardAuthFailure(const QString errMessage);

private slots:

  void handleLibSongAddError(CommErrorHandler::CommErrorType errorType);

  void onAuthenticated(const QByteArray& ticket, const user_id_t& user_id);

private:

  DataStore *dataStore;

  UDJServerConnection *serverConnection;

  bool hasPendingReauthRequest;
 
  bool syncLibOnReauth;

  void clearOnReauthFlags();  

  void requestReauth();
};

}

#endif //COMM_ERROR_HANDLER_HPP